package pl.edu.agh.to2.file;

import java.io.File;
import java.util.Collection;
import java.util.regex.Pattern;

public interface FileSystemService {
    Collection<File> searchDirectory(String path, Pattern pattern);
    boolean deleteFile(String path);
}
