package pl.edu.agh.to2.service;

import java.io.IOException;
import java.nio.file.Path;

public interface FileHasher {
    String hash(Path path) throws IOException;
}
