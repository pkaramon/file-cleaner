package pl.edu.agh.to2.example.file;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;


@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class FileServiceTest {
    @Autowired
    @InjectMocks
    private FileService fileService;

    @SpyBean
    private FileRepository fileRepository;

    @MockBean
    private FileSearch fileSearch;

    @BeforeEach
    void setupDb() {
        fileRepository.deleteAll();
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
        Pattern pattern = Pattern.compile(".*\\.txt");
        var a = Mockito.spy(new java.io.File("Documents/a.txt"));
        var b = Mockito.spy(new java.io.File("Documents/b.txt"));

        when(a.getName()).thenReturn("a.txt");
        when(a.getPath()).thenReturn("Documents/a.txt");
        when(a.length()).thenReturn(100L);
        when(a.lastModified()).thenReturn(100L);


        when(b.getName()).thenReturn("b.txt");
        when(b.getPath()).thenReturn("Documents/b.txt");
        when(b.length()).thenReturn(300L);
        when(b.lastModified()).thenReturn(300L);

        when(fileSearch.searchDirectory("Documents/", pattern)).thenReturn(
                List.of(a, b));

        // when
        fileService.loadFromPath("Documents/", pattern);

        // then
        List<File> files = fileService.findFilesInPath("Documents/");
        assertEquals(2, files.size());
        assertTrue(files.contains(fileRepository.findByPath("Documents/a.txt").get()));
        assertTrue(files.contains(fileRepository.findByPath("Documents/b.txt").get()));
    }

    @Test
    void testLoadFromPath_NoUpdateWhenLastModifiedNotChanged() {
        // given
        Pattern pattern = Pattern.compile(".*\\.txt");
        var a = Mockito.spy(new java.io.File("Documents/a.txt"));
        var b = Mockito.spy(new java.io.File("Documents/b.txt"));

        when(a.getName()).thenReturn("a.txt");
        when(a.getPath()).thenReturn("Documents/a.txt");
        when(a.length()).thenReturn(100L);
        when(a.lastModified()).thenReturn(100L);

        when(b.getName()).thenReturn("b.txt");
        when(b.getPath()).thenReturn("Documents/b.txt");
        when(b.length()).thenReturn(300L);
        when(b.lastModified()).thenReturn(300L);

        when(fileSearch.searchDirectory("Documents/", pattern)).thenReturn(
                List.of(a, b));

        fileService.loadFromPath("Documents/", pattern);

        when(a.lastModified()).thenReturn(123321L);

        // then
        fileService.loadFromPath("Documents/", pattern);

        // then
        // 2 for first time a and b, 1 for a second time a
        verify(fileRepository, times(3)).save(any());

        assertEquals(123321L, fileRepository.findByPath("Documents/a.txt").get().getLastModified());
    }


}