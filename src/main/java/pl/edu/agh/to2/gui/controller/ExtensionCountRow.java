package pl.edu.agh.to2.gui.controller;

public class ExtensionCountRow {
    private final String extension;
    private final Long count;

    public ExtensionCountRow(String extension, Long count) {
        this.extension = extension;
        this.count = count;
    }

    public String getExtension() {
        return extension;
    }

    public Long getCount() {
        return count;
    }
}
