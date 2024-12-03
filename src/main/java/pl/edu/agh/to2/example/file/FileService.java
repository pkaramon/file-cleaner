package pl.edu.agh.to2.example.file;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.edu.agh.to2.example.actionLog.ActionLog;
import pl.edu.agh.to2.example.actionLog.ActionLogRepository;
import pl.edu.agh.to2.example.actionLog.ActionType;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;
    private final FileSystemService fileSystemService;
    private final Clock clock;
    private final ActionLogRepository actionLogRepository;


    public FileService(FileRepository fileRepository,
                       FileSystemService fileSystemService,
                       Clock clock, ActionLogRepository actionLogRepository) {
        this.fileRepository = fileRepository;
        this.fileSystemService = fileSystemService;
        this.clock = clock;
        this.actionLogRepository = actionLogRepository;
    }


    @Transactional
    public void loadFromPath(String path, Pattern pattern) {
        var fileInDirectory = fileSystemService.searchDirectory(path, pattern);
        // Checking if file already exists in database, and it's state is up to date
        // is currently pretty much pointless. It would make sense if we stored lots of
        // expensive to compute data about files

        Map<String, File> fileMap = fileRepository.getMapFromPathToFile();
        Set<String> pathsInDirectory = new HashSet<>();

        addOrUpdateRecordsInDatabase(fileInDirectory, pathsInDirectory, fileMap);
        deleteRecordsInDatabaseIfNoLongerInFileSystem(fileMap, pathsInDirectory);
    }


    private void addOrUpdateRecordsInDatabase(Iterable<java.io.File> fileInDirectory,
                                              Set<String> pathsInDirectory,
                                              Map<String, File> fileMap) {
        for (java.io.File file : fileInDirectory) {
            String filePath = file.getPath();
            pathsInDirectory.add(filePath);
            File dbFile = fileMap.get(filePath);
            if (dbFile == null) {
                File newFile = new File(file.getName(), filePath, file.length(), file.lastModified());
                fileRepository.save(newFile);
                logger.info("File added: {}", newFile.getName());
            } else if (dbFile.getLastModified() != file.lastModified()) {
                dbFile.setLastModified(file.lastModified());
                dbFile.setSize(file.length());
                fileRepository.save(dbFile);
                logger.info("File updated: {}", dbFile.getName());
            } else {
                logger.info("File already up to date: {}", dbFile.getName());
            }
        }
    }

    private void deleteRecordsInDatabaseIfNoLongerInFileSystem(Map<String, File> fileMap, Set<String> pathsInDirectory) {
        List<File> filesToDelete = fileMap.values().stream()
                .filter(file -> !pathsInDirectory.contains(file.getPath()))
                .toList();

        fileRepository.deleteAll(filesToDelete);
        for (File deletedFile : filesToDelete) {
            logger.info("File deleted: {}", deletedFile.getName());
        }
    }

    public List<File> findFilesInPath(String directoryPath) {
        return fileRepository.findByPathStartingWith(directoryPath);
    }

    public List<File> findLargestFilesIn(String path, int n) {
        return fileRepository.findLargestFilesIn(path, n);
    }

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
}
