package pl.edu.agh.to2.example;

import org.springframework.stereotype.Component;
import pl.edu.agh.to2.example.file.FileSystemService;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class FileSystemServiceImp implements FileSystemService {

    @Override
    public Iterable<File> searchDirectory(String path, Pattern pattern) {
        List<File> fileList = new LinkedList<>();
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Invalid folder path provided.");
            return fileList;
        }
        search(fileList, dir, pattern);

        return fileList;
    }

    @Override
    public boolean deleteFile(String path) {
        File file = new File(path);
        try {
            return file.delete();
        } catch (Exception e) {
            System.out.println("Error occurred while deleting file: " + e.getMessage());
            return false;
        }
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
                    System.out.println("File path: " + file.getName());
                    fileList.add(file);
                }
            }
        }
    }

}
