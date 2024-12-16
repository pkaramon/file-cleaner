package pl.edu.agh.to2.service;

import java.io.IOException;

public interface FileHasher {
    String hash(String path) throws IOException;
}
