package pl.edu.agh.to2.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.edu.agh.to2.model.ActionLog;
import pl.edu.agh.to2.repository.ActionLogRepository;
import pl.edu.agh.to2.types.ActionType;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.repository.FileRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
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
        // NOTE: Creating a new database from the files in the directory
        // is cheap for now, because we are only storing basic information about the files.
        // We may want to consider a more efficient way of doing this if
        // some metadata is expensive to compute.
        fileRepository.deleteAll();
        fileRepository.flush();
        var files = fileSystemService.searchDirectory(path, pattern);

        var fileRecords = files.stream()
                .map(f -> new File(f.getName(), f.getPath(), f.length(), f.lastModified()))
                .toList();

        fileRepository.saveAll(fileRecords);

        logger.info("Files loaded from path: {}. Number of files: {}", path, fileRecords.size());
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
}
