package pl.edu.agh.to2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class FileSystemServiceImp implements FileSystemService {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemServiceImp.class);

    @Override
    public Collection<File> searchDirectory(String path, Pattern pattern) {
        List<File> fileList = new LinkedList<>();
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.info("Invalid folder path provided.");
            return fileList;
        }
        search(fileList, dir, pattern);

        return fileList;
    }

    private void search(List<File> fileList, File dir, Pattern pattern) {
        File[] filesList = dir.listFiles();
        if (filesList == null) {
            return;
        }
        for (File file : filesList) {
            if (file.isDirectory()) {
                search(fileList, file, pattern);
            } else {
                if (pattern == null || pattern.matcher(file.getName()).matches()) {
                    logger.info("File path: {}", file.getName());
                    fileList.add(file);
                }
            }
        }
    }

    @Override
    public boolean deleteFile(String path) {
        File file = new File(path);
        try {
            return file.delete();
        } catch (Exception e) {
            logger.info("Error occurred while deleting file: {}", e.getMessage());
            return false;
        }
    }


}
