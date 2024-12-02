package pl.edu.agh.to2.example.file;

import java.io.File;

public interface FileSearch {
    Iterable<File> searchDirectory(String path);
}
