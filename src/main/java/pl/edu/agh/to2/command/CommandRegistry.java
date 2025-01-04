package pl.edu.agh.to2.command;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CommandRegistry {

    private final ObservableList<Command> commandStack = FXCollections
            .observableArrayList();

    private final ObservableList<Command> redoCommandStack = FXCollections
            .observableArrayList();

    public void executeCommand(Command command) {
        command.execute();
        commandStack.add(command);
        redoCommandStack.clear();
    }
    public void undo() {
        if(commandStack.isEmpty()) {
            return;
        }
        Command lastCommand = commandStack.remove(commandStack.size() - 1);
        lastCommand.undo();
        redoCommandStack.add(lastCommand);
    }

    public void redo() {
        if(redoCommandStack.isEmpty()) {
            return;
        }
        Command lastCommand = redoCommandStack.remove(redoCommandStack.size() - 1);
        lastCommand.redo();
        commandStack.add(lastCommand);
    }

    public boolean canUndo() {
        return !commandStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoCommandStack.isEmpty();
    }
}
