package pl.edu.agh.to2.example.file;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileRepository fileRepository;
    private final FileSearch fileSearch;


    public FileService(FileRepository fileRepository, FileSearch fileSearch) {
        this.fileRepository = fileRepository;
        this.fileSearch = fileSearch;
    }


    @Transactional
    public void loadFromPath(String path, Pattern pattern) {
        var files = fileSearch.searchDirectory(path, pattern);
        // This checking if file already exists in database, and it's state is up to date
        // is currently pretty much pointless. It would make sense if we stored lots of
        // expensive to compute data about files

        Map<String, File> fileMap = fileRepository.getMapFromPathToFile();
        for (java.io.File file : files) {
            if (fileMap.containsKey(file.getPath())) {
                var fileEntity = fileMap.get(file.getPath());
                if (fileEntity.getLastModified() != file.lastModified()) {
                    fileEntity.setLastModified(file.lastModified());
                    fileEntity.setSize(file.length());
                    fileRepository.save(fileEntity);
                    logger.info("File updated: {}", fileEntity.getName());
                } else {
                    logger.info("File already exists and is up to date: {}", fileEntity.getName());
                }
            } else {
                File fileEntity = new File(file.getName(), file.getPath(), file.length(), file.lastModified());
                fileRepository.save(fileEntity);
                logger.info("File saved: {}", fileEntity.getName());
            }

        }
    }

    public List<File> findFilesInPath(String directoryPath) {
        return fileRepository.findByPathStartingWith(directoryPath);
    }

    public List<File> findLargestFilesIn(String path, int n) {
        return fileRepository.findLargestFilesIn(path, n);
    }
}
