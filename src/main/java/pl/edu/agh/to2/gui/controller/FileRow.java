package pl.edu.agh.to2.gui.controller;

import pl.edu.agh.to2.model.File;

public class FileRow {
    private final String path;
    private final String size;
    private final String hash;

    public FileRow(File file) {
        this(String.valueOf(file.getPath()), file.getSize() + " bytes", file.getHash());
    }

    public FileRow(String path, String size, String hash) {
        this.path = path;
        this.size = size;
        this.hash = hash;
    }

    public String getPath() {
        return path;
    }

    public String getSize() {
        return size;
    }

    public String getHash() {
        return hash;
    }
}
