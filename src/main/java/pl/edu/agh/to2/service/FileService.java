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
import pl.edu.agh.to2.repository.FileSizeStats;
import pl.edu.agh.to2.types.ActionType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;
    private final Clock clock;
    private final ActionLogRepository actionLogRepository;
    private final FileHasher fileHasher;

    public FileService(FileRepository fileRepository,
                       Clock clock,
                       ActionLogRepository actionLogRepository,
                       FileHasher fileHasher) {
        this.fileRepository = fileRepository;
        this.clock = clock;
        this.actionLogRepository = actionLogRepository;
        this.fileHasher = fileHasher;
    }

    @Transactional
    public void loadFromPath(Path root, Pattern pattern) {
        fileRepository.deleteAll();
        fileRepository.flush();

        List<Path> files = searchDirectory(root, pattern);
        List<File> fileRecords = new ArrayList<>();
        for (Path file : files) {
            try {
                fileRecords.add(new File(
                        file.getFileName().toString(),
                        file.toString(),
                        Files.size(file),
                        Files.getLastModifiedTime(file).toMillis(),
                        tryToHash(file)
                ));
            } catch (IOException e) {
                logger.error("Error loading file: {}", file, e);
            }
        }

        fileRepository.saveAll(fileRecords);
        logger.info("Files loaded from path: {}. Number of files: {}", root, fileRecords.size());
    }

    private List<Path> searchDirectory(Path path, Pattern pattern) {
        List<Path> fileList = new LinkedList<>();
        if (Files.notExists(path) || !Files.isDirectory(path)) {
            logger.info("Invalid folder path provided.");
            return fileList;
        }
        search(fileList, path, pattern);
        return fileList;
    }

    private void search(List<Path> resultsList, Path dir, Pattern pattern) {
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> filesList = files.toList();
            for (Path file : filesList) {
                handleFile(resultsList, pattern, file);
            }
        } catch (IOException ignored) {
            // We ignore the exception because we don't want to interrupt the program's execution in case of errors while listing files in the directory
        }
    }

    private void handleFile(List<Path> resultsList, Pattern pattern, Path file) {
        if (Files.isDirectory(file)) {
            search(resultsList, file, pattern);
        } else {
            String fileName = file.getFileName().toString();
            if (pattern == null || pattern.matcher(fileName).matches()) {
                logger.info("File path: {}", file);
                resultsList.add(file);
            }
        }
    }

    private String tryToHash(Path path) {
        try {
            return fileHasher.hash(path);
        } catch (IOException e) {
            logger.error("Error hashing file: {}", path, e);
            throw new RuntimeException("Error hashing file: " + path, e);
        }
    }

    public List<File> findFilesInPath(Path directoryPath) {
        return fileRepository.findByPathStartingWith(String.valueOf(directoryPath));
    }

    public List<File> findLargestFilesIn(Path path, int n) {
        return fileRepository.findLargestFilesIn(String.valueOf(path), n);
    }

    public List<List<File>> findDuplicatedGroups() {
        List<File> duplicates = fileRepository.findDuplicates();
        return new ArrayList<>(duplicates.stream()
                .collect(Collectors.groupingBy(f -> f.getHash() + "_" + f.getSize()))
                .values());
    }

    @Transactional
    public void deleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            logger.error("Error deleting file: {}", path, e);
            return;
        }
        fileRepository.deleteByPath(path.toString());

        logger.info("File deleted: {}", path);

        ActionLog actionLog = new ActionLog(
                ActionType.DELETE,
                "File deleted: " + path,
                LocalDateTime.now(clock)
        );

        actionLogRepository.save(actionLog);
    }

    @Transactional
    public void archiveFiles(List<File> files, Path zipFilePath) throws IOException {
        try (OutputStream outStream = Files.newOutputStream(zipFilePath);
             ZipOutputStream zipOut = new ZipOutputStream(outStream)) {
            Map<String, Integer> filenameToCount = new HashMap<>();

            for (File file : files) {
                zipFile(file, zipOut, zipFilePath.getFileSystem(), filenameToCount);
            }
            logger.info("Files archived to: {}", zipFilePath);
            ActionLog actionLog = new ActionLog(
                    ActionType.ARCHIVE,
                    "Files archived to: " + zipFilePath,
                    LocalDateTime.now(clock)
            );
            actionLogRepository.save(actionLog);
        } catch (IOException e) {
            logger.error("Error creating ZIP file at: {}", zipFilePath);
            throw e;
        }
    }

    private void zipFile(File file,
                         ZipOutputStream zipOut,
                         FileSystem fileSystem,
                         Map<String, Integer> basenameToCount) throws IOException {
        String entryName = file.getName();
        if (basenameToCount.containsKey(entryName)) {
            int count = basenameToCount.get(entryName) + 1;
            entryName =
                    "%s(%d).%s".formatted(getWithoutExtension(file.getName()), count, getExtension(file.getName()));
            basenameToCount.put(file.getName(), count);
        } else {
            basenameToCount.put(file.getName(), 1);
        }

        Path entryPath = fileSystem.getPath(file.getPath());
        try (InputStream in = Files.newInputStream(entryPath)) {
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
            throw e;
        }
    }

    private String getWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public List<List<File>> findVersions(int maxDistance) {
        List<EditDistanceResult> similarFiles = fileRepository.findSimilarFileNames(maxDistance);
        List<Set<File>> versionGroups = new ArrayList<>();

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

        return new ArrayList<>(versionGroups
                .stream()
                .filter(group -> group.size() > 1)
                .map(ArrayList::new)
                .toList()
        );
    }

    public Optional<FileSizeStats> getFileSizeStats() {
        FileSizeStats stats = fileRepository.findFileSizeStats();
        return stats.count() == 0 ? Optional.empty() : Optional.of(stats);
    }

    public Optional<Histogram> getFileSizeHistogram(int bucketCount) {
        List<Long> sizes = fileRepository.findAllSizes();
        return calculateHistogram(bucketCount, sizes);
    }

    public Optional<Histogram> getLastModifiedHistogram(int bucketCount) {
        List<Long> lastModified = fileRepository.findAllLastModifiedInMilliseconds();
        return calculateHistogram(bucketCount, lastModified);
    }

    private Optional<Histogram> calculateHistogram(int bucketCount, List<Long> data) {
        if (data.isEmpty()) {
            return Optional.empty();
        }
        long min = data.stream().min(Long::compareTo).orElseThrow();
        long max = data.stream().max(Long::compareTo).orElseThrow();
        double bucketSize = (double) (max - min) / bucketCount;
        List<Long> buckets = new ArrayList<>(Collections.nCopies(bucketCount, 0L));
        for (Long value : data) {
            int bucketIndex = (int) ((value - min) / bucketSize);
            if (bucketIndex == bucketCount) {
                bucketIndex--;
            }
            buckets.set(bucketIndex, buckets.get(bucketIndex) + 1);
        }
        return Optional.of(new Histogram(min, max, buckets));
    }

    public Map<String, Long> getFileCountsByExtension() {
        return fileRepository.findFileCountsByExtension();
    }
}

