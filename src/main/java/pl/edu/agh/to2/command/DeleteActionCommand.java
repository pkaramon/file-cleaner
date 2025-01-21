package pl.edu.agh.to2.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.model.ActionLog;
import pl.edu.agh.to2.repository.ActionLogRepository;
import pl.edu.agh.to2.service.FileService;
import pl.edu.agh.to2.types.ActionType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class DeleteActionCommand implements Command {

    private final Path trashFolderPath = Paths.get(System.getProperty("user.dir"), ".trash").toAbsolutePath();
    private Path fileToBeDeleted;
    private final FileService fileService;
    private final ActionLogRepository actionLogRepository;

    private final Clock clock;

    @Autowired
    public DeleteActionCommand(FileService fileService, ActionLogRepository actionLogRepository, Clock clock) {
        this.fileService = fileService;
        this.actionLogRepository = actionLogRepository;
        this.clock = clock;

    }
    public void setFileToBeDeleted(Path fileToBeDeleted) {
        this.fileToBeDeleted = fileToBeDeleted;
    }
    @Override
    public void execute() {
        if (!Files.exists(trashFolderPath)) {
            try {
                Files.createDirectory(trashFolderPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Files.copy(fileToBeDeleted, trashFolderPath.resolve(fileToBeDeleted.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fileService.deleteFile(fileToBeDeleted);
    }

    @Override
    public void undo() {
        try {
            Files.move(trashFolderPath.resolve(fileToBeDeleted.getFileName()), fileToBeDeleted);
            ActionLog actionLog = new ActionLog(
                    ActionType.DELETE_UNDO,
                    "Undo delete file: " + fileToBeDeleted,
                    LocalDateTime.now(clock)
            );
            actionLogRepository.save(actionLog);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void redo() {
        this.execute();
    }
}
