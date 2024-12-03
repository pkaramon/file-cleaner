package pl.edu.agh.to2.example.file;

import java.io.File;
import java.util.regex.Pattern;

public interface FileSearch {
    Iterable<File> searchDirectory(String path, Pattern pattern);
}
