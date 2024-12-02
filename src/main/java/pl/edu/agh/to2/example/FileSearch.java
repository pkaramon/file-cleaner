package pl.edu.agh.to2.example;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class FileSearch {

    private Pattern pattern;
    public FileSearch(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public FileSearch() {
    }

    public List<File> searchDirectory(String path) {
        List<File> fileList = new LinkedList<>();
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Invalid folder path provided.");
            return fileList;
        }
        search(fileList, dir);

        return fileList;
    }

    private void search(List<File> fileList, File dir) {
        File[] filesList = dir.listFiles();
        if(filesList == null) {
            return;
        }
        for(File file : filesList) {
            if(file.isDirectory()) {
                search(fileList, file);
            } else {
                if(pattern == null || pattern.matcher(file.getName()).matches()) {
                    System.out.println("File path: "+file.getName());
                    fileList.add(file);
                }
            }
        }
    }


}
