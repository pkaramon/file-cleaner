package pl.edu.agh.to2.service;

public record FileInfo(String path,
                       long size,
                       long lastModified
) {
}
