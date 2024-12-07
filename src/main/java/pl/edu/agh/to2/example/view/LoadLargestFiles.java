package pl.edu.agh.to2.example.view;

import javafx.concurrent.Task;
import pl.edu.agh.to2.example.file.File;
import pl.edu.agh.to2.example.file.FileService;

import java.util.List;
import java.util.regex.Pattern;


public class LoadLargestFiles extends Task<List<String>> {
    private final String directoryPath;
    private final FileService fileService;

    public LoadLargestFiles(String directoryPath, FileService fileService) {
        this.directoryPath = directoryPath;
        this.fileService = fileService;
    }

    @Override
    protected List<String> call() {
        // Load files from path
        fileService.loadFromPath(directoryPath, Pattern.compile(".*"));

        // Simulate some processing and updating progress
        List<File> files = fileService.findLargestFilesIn(directoryPath, 10);
        return files.stream()
                .map(file -> "%s (%d bytes)".formatted(file.getPath(), file.getSize()))
                .toList();
    }
}
