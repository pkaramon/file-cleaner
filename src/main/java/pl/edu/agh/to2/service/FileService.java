package pl.edu.agh.to2.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.edu.agh.to2.model.ActionLog;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.repository.ActionLogRepository;
import pl.edu.agh.to2.repository.FileRepository;
import pl.edu.agh.to2.types.ActionType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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
        // NOTE: Creating a new database from the files in the directory
        // is cheap for now, because we are only storing basic information about the files.
        // We may want to consider a more efficient way of doing this if
        // some metadata is expensive to compute.
        fileRepository.deleteAll();
        fileRepository.flush();
        var files = fileSystemService.searchDirectory(path, pattern);

        var fileRecords = files.stream()
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

    @Transactional
    public void deleteFile(String path) {
        fileSystemService.deleteFile(path);
        fileRepository.deleteByPath(path);

        logger.info("File deleted: {}", path);

        ActionLog actionLog = new ActionLog(
                ActionType.DELETE,
                "File deleted: " + path,
                LocalDateTime.now(clock)
        );

        actionLogRepository.save(actionLog);
    }

    public Map<Long, List<File>> findDuplicates() {
        List<File> allFiles = fileRepository.findAll();

        return allFiles.stream()
                .collect(Collectors.groupingBy(File::getSize))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1) // Filtrujemy tylko grupy o więcej niż jednym elemencie
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    public void archiveDuplicates(Map<Long, List<File>> duplicates, java.io.File selectedDirectory) throws IOException {
        // Utwórz nazwę pliku ZIP
        String zipFileName = "duplicates_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip";
        java.io.File zipFile = new java.io.File(selectedDirectory, zipFileName);

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            // Iteracja po duplikatach
            for (List<File> duplicateFiles : duplicates.values()) {
                for (File file : duplicateFiles) {
                    java.io.File inputFile = new java.io.File(file.getPath()); // Użycie `java.io.File`

                    // Sprawdź, czy plik istnieje
                    if (inputFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(inputFile)) {
                            ZipEntry zipEntry = new ZipEntry(inputFile.getName());
                            zipOut.putNextEntry(zipEntry);

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = fis.read(buffer)) >= 0) {
                                zipOut.write(buffer, 0, length);
                            }

                            zipOut.closeEntry();
                        }
                    } else {
                        throw new IOException("File not found: " + inputFile.getPath());
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException("Error archiving duplicate files: " + e.getMessage(), e);
        }
    }

}
