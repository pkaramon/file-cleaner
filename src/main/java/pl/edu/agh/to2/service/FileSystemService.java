package pl.edu.agh.to2.service;

import java.util.Collection;
import java.util.regex.Pattern;

public interface FileSystemService {
    Collection<FileInfo> searchDirectory(String path, Pattern pattern);
    boolean deleteFile(String path);
}
