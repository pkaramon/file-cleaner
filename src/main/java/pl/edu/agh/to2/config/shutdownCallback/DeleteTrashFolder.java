package pl.edu.agh.to2.config.shutdownCallback;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DeleteTrashFolder {
    private final Path trashFolderPath = Paths.get(System.getProperty("user.dir"), ".trash").toAbsolutePath();
    @PreDestroy
    public void destroy() {
        FileSystemUtils.deleteRecursively(trashFolderPath.toFile());
    }
}
