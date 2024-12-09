package pl.edu.agh.to2.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.repository.ActionLogRepository;
import pl.edu.agh.to2.types.ActionType;
import pl.edu.agh.to2.repository.FileRepository;
import pl.edu.agh.to2.service.FileService;
import pl.edu.agh.to2.service.FileSystemService;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class FileServiceTest {
    private static final Pattern defaultPattern = Pattern.compile(".*");
    @Autowired
    @InjectMocks
    private FileService fileService;

    @SpyBean
    private FileRepository fileRepository;

    @MockBean
    private FileSystemService fileSystemService;

    @SpyBean
    private ActionLogRepository actionLogRepository;

    @MockBean
    private Clock clock;

    private static java.io.File createSpyFile(String path, long size, long lastModified) {
        var a = Mockito.spy(new java.io.File(path));
        when(a.getName()).thenReturn(Path.of(path).getFileName().toString());
        when(a.getPath()).thenReturn(path);
        when(a.length()).thenReturn(size);
        when(a.lastModified()).thenReturn(lastModified);
        return a;
    }

    @BeforeEach
    void setupDb() {
        fileRepository.deleteAll();
        actionLogRepository.deleteAll();
    }

    void addSampleDataToDatabase() {
        File a = new File("a.txt", "Documents/a.txt", 100, 100);
        File b = new File("b.txt", "Documents/b.txt", 200, 200);
        File c = new File("c.txt", "Documents/c.txt", 300, 300);
        File d = new File("notes.txt", "Desktop/notes.txt", 400, 400);

        fileRepository.saveAll(List.of(a, b, c, d));
    }

    @Test
    void testFindLargestFiles() {
        addSampleDataToDatabase();

        List<File> largestFiles = fileService.findLargestFilesIn("Documents/", 2);

        assertEquals(
                largestFiles.stream().map(File::getName).toList(),
                List.of("c.txt", "b.txt")
        );
    }

    @Test
    void testLoadFromPath_WhenDatabaseIsEmpty() {
        // given
        var a = createSpyFile("Documents/a.txt", 100, 100);
        var b = createSpyFile("Documents/b.txt", 300, 300);

        when(fileSystemService.searchDirectory("Documents/", defaultPattern)).thenReturn(
                List.of(a, b));

        // when
        fileService.loadFromPath("Documents/", defaultPattern);

        // then
        List<File> files = fileService.findFilesInPath("Documents/");
        assertEquals(2, files.size());
        assertTrue(files.contains(fileRepository.findByPath("Documents/a.txt").get()));
        assertTrue(files.contains(fileRepository.findByPath("Documents/b.txt").get()));
    }

    @Test
    void testLoadFromPath_WhenFileIsDeletedFromDirectoryItsAlsoDeletedFromDb() {
        // given
        var a = createSpyFile("Documents/a.txt", 100, 100);

        when(fileSystemService.searchDirectory("Documents/", defaultPattern)).thenReturn(List.of(a));

        fileService.loadFromPath("Documents/", defaultPattern);

        when(fileSystemService.searchDirectory("Documents/", defaultPattern)).thenReturn(List.of());

        // when
        fileService.loadFromPath("Documents/", defaultPattern);

        // then
        assertTrue(fileRepository.findByPath("Documents/a.txt").isEmpty());
    }

    @Test
    void testDeleteFile() {
        // given
        var a = createSpyFile("Documents/a.txt", 100, 100);
        when(fileSystemService.searchDirectory("Documents/", defaultPattern)).thenReturn(List.of(a));
        fileService.loadFromPath("Documents/", defaultPattern);
        when(fileSystemService.deleteFile("Documents/a.txt")).thenReturn(true);
        when(clock.instant()).thenReturn(Instant.parse("2024-01-01T07:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UCT"));

        // when
        fileService.deleteFile("Documents/a.txt");

        // then
        assertTrue(fileRepository.findByPath("Documents/a.txt").isEmpty());
        verify(fileSystemService).deleteFile("Documents/a.txt");

        var log = actionLogRepository.findAll().get(0);
        assertEquals("File deleted: Documents/a.txt", log.getDescription());
        Assertions.assertEquals(ActionType.DELETE, log.getActionType());
        var expectedDateTime = Instant.parse("2024-01-01T07:00:00Z")
                .atZone(ZoneId.of("UCT"))
                .toLocalDateTime();
        assertEquals(expectedDateTime, log.getTimestamp());
    }
}