package pl.edu.agh.to2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class FileSystemServiceImp implements FileSystemService {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemServiceImp.class);

    @Override
    public Collection<FileInfo> searchDirectory(String path, Pattern pattern) {
        List<FileInfo> fileList = new LinkedList<>();
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.info("Invalid folder path provided.");
            return fileList;
        }
        search(fileList, dir, pattern);

        return fileList;
    }

    private void search(List<FileInfo> fileList, File dir, Pattern pattern) {
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
                    fileList.add(new FileInfo(file.getPath(), file.length(), file.lastModified()));
                }
            }
        }
    }

    @Override
    public void deleteFile(String path) throws IOException {
        File file = new File(path);
        if (!file.delete()) {
            throw new IOException("Failed to delete file: " + path);
        }
    }

    @Override
    public OutputStream openFileForWrite(String path) throws IOException {
        return new FileOutputStream(path);
    }

    @Override
    public InputStream openFileForRead(String path) throws IOException {
        return new FileInputStream(path);
    }
}
