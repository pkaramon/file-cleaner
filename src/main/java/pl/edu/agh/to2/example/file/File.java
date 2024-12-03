package pl.edu.agh.to2.example.file;

import jakarta.persistence.*;

@Entity
@Table(
        indexes = {
                @Index(name = "idx_file_name", columnList = "name"),
                @Index(name = "idx_file_path", columnList = "path"),
                @Index(name = "idx_file_size", columnList = "size"),
                @Index(name = "idx_file_last_modified", columnList = "lastModified")
        }
)
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String path;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private long lastModified;

    public File() {

    }

    public File(String name, String path, long size, long lastModified) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
