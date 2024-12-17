package pl.edu.agh.to2.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.edu.agh.to2.model.ActionLog;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.repository.ActionLogRepository;
import pl.edu.agh.to2.repository.EditDistanceResult;
import pl.edu.agh.to2.repository.FileRepository;
import pl.edu.agh.to2.types.ActionType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;
    private final FileSystemService fileSystemService;
    private final Clock clock;
    private final ActionLogRepository actionLogRepository;
    private final FileHasher fileHasher;


    public FileService(FileRepository fileRepository,
                       FileSystemService fileSystemService,
                       Clock clock,
                       ActionLogRepository actionLogRepository,
                       FileHasher fileHasher) {
        this.fileRepository = fileRepository;
        this.fileSystemService = fileSystemService;
        this.clock = clock;
        this.actionLogRepository = actionLogRepository;
        this.fileHasher = fileHasher;
    }


    @Transactional
    public void loadFromPath(String path, Pattern pattern) {
        fileRepository.deleteAll();
        fileRepository.flush();

        Collection<FileInfo> files = fileSystemService.searchDirectory(path, pattern);

        List<File> fileRecords = files.stream()
                .map(f -> new File(
                        Path.of(f.path()).getFileName().toString(),
                        f.path(),
                        f.size(),
                        f.lastModified(),
                        tryToHash(f.path())))
                .toList();

        fileRepository.saveAll(fileRecords);

        logger.info("Files loaded from path: {}. Number of files: {}", path, fileRecords.size());
    }

    private String tryToHash(String path) {
        try {
            return fileHasher.hash(path);
        } catch (IOException e) {
            logger.error("Error hashing file: {}", path, e);
            throw new RuntimeException("Error hashing file: " + path, e);
        }
    }

    public List<File> findFilesInPath(String directoryPath) {
        return fileRepository.findByPathStartingWith(directoryPath);
    }

    public List<File> findLargestFilesIn(String path, int n) {
        return fileRepository.findLargestFilesIn(path, n);
    }


    public List<List<File>> findDuplicatedGroups() {
        List<File> duplicates = fileRepository.findDuplicates();
        return new ArrayList<>(duplicates.stream()
                .collect(Collectors.groupingBy(f -> f.getHash() + "_" + f.getSize()))
                .values());
    }

    @Transactional
    public void deleteFile(String path) {
        try {
            fileSystemService.deleteFile(path);
        } catch (IOException e) {
            logger.error("Error deleting file: {}", path, e);
            return;
        }
        fileRepository.deleteByPath(path);

        logger.info("File deleted: {}", path);

        ActionLog actionLog = new ActionLog(
                ActionType.DELETE,
                "File deleted: " + path,
                LocalDateTime.now(clock)
        );

        actionLogRepository.save(actionLog);
    }

    @Transactional
    public void deleteDuplicates(Map<Long, List<File>> duplicates) {
        // Iteracja po grupach duplikatów
        for (Map.Entry<Long, List<File>> entry : duplicates.entrySet()) {
            List<File> duplicateFiles = entry.getValue();

            // Usuwamy wszystkie pliki oprócz jednego
            if (duplicateFiles.size() > 1) {
                for (int i = 1; i < duplicateFiles.size(); i++) {
                    File file = duplicateFiles.get(i);
                    deleteFile(file.getPath());  // Wywołanie metody usuwania pliku
                }
            }
        }
    }

    public void archiveFiles(List<File> files, String zipFilePath) throws IOException {
        try (OutputStream outStream = fileSystemService.openFileForWrite(zipFilePath);
             ZipOutputStream zipOut = new ZipOutputStream(outStream)) {
            for (File file : files) {
                String entryName = file.getName();
                try (InputStream in = fileSystemService.openFileForRead(file.getPath())) {
                    ZipEntry zipEntry = new ZipEntry(entryName);
                    zipOut.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, length);
                    }

                    zipOut.closeEntry();
                } catch (IOException e) {
                    logger.error("Failed to archive file: {}", file.getPath());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            logger.error("Error creating ZIP file at: {}", zipFilePath);
            throw e;
        }
    }

    public List<Set<File>> findVersions(int maxDistance) {
        List<EditDistanceResult> similarFiles = fileRepository.findSimilarFileNames(maxDistance);
        List<Set<File>> versionGroups = new ArrayList<>();

        // Process the results to group files by similarity
        for (EditDistanceResult res : similarFiles) {
            File first = res.first();
            File second = res.second();

            boolean addedToGroup = false;
            for (Set<File> group : versionGroups) {
                if (group.contains(first) || group.contains(second)) {
                    group.add(first);
                    group.add(second);
                    addedToGroup = true;
                    break;
                }
            }

            if (!addedToGroup) {
                Set<File> newGroup = new HashSet<>();
                newGroup.add(first);
                newGroup.add(second);
                versionGroups.add(newGroup);
            }
        }

        return versionGroups;
    }
}
