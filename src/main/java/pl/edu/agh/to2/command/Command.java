package pl.edu.agh.to2.command;

public interface Command {
    void execute();

    public void undo();

    public void redo();
}
