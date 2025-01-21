package pl.edu.agh.to2.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CommandRegistryTest {

    private CommandRegistry commandRegistry;
    private Command mockCommand;

    @BeforeEach
    void setUp() {
        commandRegistry = new CommandRegistry();
        mockCommand = mock(Command.class);
    }

    @Test
    void testExecuteCommand() {
        commandRegistry.executeCommand(mockCommand);
        verify(mockCommand).execute();
        assertTrue(commandRegistry.canUndo());
        assertFalse(commandRegistry.canRedo());
    }

    @Test
    void testUndo() {
        commandRegistry.executeCommand(mockCommand);
        commandRegistry.undo();
        verify(mockCommand).undo();
        assertFalse(commandRegistry.canUndo());
        assertTrue(commandRegistry.canRedo());
    }

    @Test
    void testRedo() {
        commandRegistry.executeCommand(mockCommand);
        commandRegistry.undo();
        commandRegistry.redo();
        verify(mockCommand).redo();
        assertTrue(commandRegistry.canUndo());
        assertFalse(commandRegistry.canRedo());
    }

    @Test
    void testUndoWhenStackIsEmpty() {
        commandRegistry.undo();
        assertFalse(commandRegistry.canUndo());
        assertFalse(commandRegistry.canRedo());
    }

    @Test
    void testRedoWhenStackIsEmpty() {
        commandRegistry.redo();
        assertFalse(commandRegistry.canUndo());
        assertFalse(commandRegistry.canRedo());
    }
}
