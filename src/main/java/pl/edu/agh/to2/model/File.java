package pl.edu.agh.to2.model;

import jakarta.persistence.*;

import java.util.Objects;

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

    @Column(nullable = false, unique = true, length = 1024)
    private String path;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private long lastModified;

    @Column(nullable = false)
    private String hash;

    public File() {

    }

    public File(String name, String path, long size, long lastModified, String hash) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
        this.hash = hash;
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

    public String getHash() {
        return hash;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        File file = (File) o;
        return getId() == file.getId() &&
                getSize() == file.getSize() &&
                getLastModified() == file.getLastModified()
                && Objects.equals(getName(), file.getName())
                && Objects.equals(getPath(), file.getPath())
                && Objects.equals(getHash(), file.getHash());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getId(),
                getName(),
                getPath(),
                getSize(),
                getLastModified(),
                getHash()
        );
    }
}
