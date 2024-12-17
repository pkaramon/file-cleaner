package pl.edu.agh.to2.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.regex.Pattern;

public interface FileSystemService {
    Collection<FileInfo> searchDirectory(String path, Pattern pattern);

    void deleteFile(String path) throws IOException;

    OutputStream openFileForWrite(String path) throws IOException;

    InputStream openFileForRead(String path) throws IOException;
}
