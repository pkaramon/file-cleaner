package pl.edu.agh.to2.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import pl.edu.agh.to2.model.File;
import pl.edu.agh.to2.repository.ActionLogRepository;
import pl.edu.agh.to2.repository.FileRepository;
import pl.edu.agh.to2.types.ActionType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FileServiceTest {
    private static final Pattern defaultPattern = Pattern.compile(".*");

    @Autowired
    @InjectMocks
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ActionLogRepository actionLogRepository;

    @MockBean
    private FileSystemService fileSystemService;

    @MockBean
    private FileHasher fileHasher;

    @MockBean
    private Clock clock;


    @BeforeEach
    void setup() throws IOException {
        fileRepository.deleteAll();
        actionLogRepository.deleteAll();

        when(fileHasher.hash(Mockito.any())).thenReturn("hash");
    }

    void addSampleDataToDatabase() {
        File a = new File("a.txt", "Docs/a.txt", 100, 100, "a");
        File b = new File("b.txt", "Docs/b.txt", 200, 200, "b");
        File c = new File("c.txt", "Docs/c.txt", 300, 300, "c");
        File d = new File("notes.txt", "Desktop/notes.txt", 400, 400, "d");

        fileRepository.saveAll(List.of(a, b, c, d));
    }

    FileInfo exampleFileInfo(String path, long size, long lastModified) {
        return new FileInfo(path, size, lastModified);
    }

    FileInfo exampleFileInfo(String path) {
        return exampleFileInfo(path, 100, 100);
    }

    void setupFileHashes(List<FileInfo> files, List<String> hashes) {
        for (int i = 0; i < files.size(); i++) {
            try {
                when(fileHasher.hash(String.valueOf(files.get(i).path()))).thenReturn(hashes.get(i));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Test
    void testFindLargestFiles() {
        addSampleDataToDatabase();

        List<File> largestFiles = fileService.findLargestFilesIn("Docs/", 2);

        assertEquals(
                largestFiles.stream().map(File::getName).toList(),
                List.of("c.txt", "b.txt")
        );
    }

    @Test
    void testLoadFromPath_WhenDatabaseIsEmpty_AddsAllTheFiles() {
        // given
        var a = exampleFileInfo("Docs/a.txt");
        var b = exampleFileInfo("Docs/b.txt");

        when(fileSystemService.searchDirectory("Docs/", defaultPattern))
                .thenReturn(List.of(a, b));

        // when
        fileService.loadFromPath("Docs/", defaultPattern);

        // then
        List<File> files = fileService.findFilesInPath("Docs/");
        assertEquals(2, files.size());
        assertTrue(files.contains(fileRepository.findByPath("Docs/a.txt").get()));
        assertTrue(files.contains(fileRepository.findByPath("Docs/b.txt").get()));
    }

    @Test
    void testLoadFromPath_WhenFileIsDeletedFromDirectory_ItsAlsoDeletedFromDb() {
        // given
        var a = exampleFileInfo("Docs/a.txt");
        when(fileSystemService.searchDirectory("Docs/", defaultPattern)).thenReturn(List.of(a));
        fileService.loadFromPath("Docs/", defaultPattern);
        when(fileSystemService.searchDirectory("Docs/", defaultPattern)).thenReturn(List.of());

        // when
        fileService.loadFromPath("Docs/", defaultPattern);

        // then
        assertTrue(fileRepository.findByPath("Docs/a.txt").isEmpty());
    }

    @Test
    void testLoadFromPath_ComputesHashForNewFiles() throws IOException {
        // given
        var a = exampleFileInfo("Docs/a.txt");
        setupFileHashes(List.of(a), List.of("a"));
        when(fileSystemService.searchDirectory("Docs/", defaultPattern)).thenReturn(List.of(a));

        // when
        fileService.loadFromPath("Docs/", defaultPattern);

        // then
        verify(fileHasher).hash("Docs/a.txt");
        var fileA = fileRepository.findByPath("Docs/a.txt").get();
        assertEquals("a", fileA.getHash());
    }

    // TODO: "improve" performance by only amending changed files
    @Test
    void testLoadFromPath_SomeFilesHaveChanged_ItUpdatesAllFiles() throws IOException {
        // given
        var a = exampleFileInfo("Docs/a.txt", 100, 100);
        var b = exampleFileInfo("Docs/b.txt", 200, 200);
        var c = exampleFileInfo("Docs/c.txt", 300, 300);
        var d = exampleFileInfo("Docs/d.txt", 400, 400);
        when(fileSystemService.searchDirectory("Docs/", defaultPattern)).thenReturn(List.of(a, b, c, d));

        fileService.loadFromPath("Docs/", defaultPattern);

        var aChanged = exampleFileInfo("Docs/a.txt", 100, 200);
        var bChanged = exampleFileInfo("Docs/b.txt", 200, 200);
        when(fileSystemService.searchDirectory("Docs/", defaultPattern)).thenReturn(List.of(aChanged, bChanged, c));

        // when
        fileService.loadFromPath("Docs/", defaultPattern);

        // then
        assertEquals(3, fileRepository.findAll().size());
        verify(fileHasher, times(2)).hash("Docs/a.txt");
        verify(fileHasher, times(2)).hash("Docs/b.txt");
        verify(fileHasher, times(2)).hash("Docs/c.txt");
        verify(fileHasher, times(1)).hash("Docs/d.txt");

    }

    @Test
    void testDeleteFile() throws IOException {
        // given
        var a = exampleFileInfo("Docs/a.txt", 100, 100);
        when(fileSystemService.searchDirectory("Docs/", defaultPattern)).thenReturn(List.of(a));
        fileService.loadFromPath("Docs/", defaultPattern);

        when(clock.instant()).thenReturn(Instant.parse("2024-01-01T07:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UCT"));

        // when
        fileService.deleteFile("Docs/a.txt");

        // then
        assertTrue(fileRepository.findByPath("Docs/a.txt").isEmpty());
        verify(fileSystemService).deleteFile("Docs/a.txt");

        var log = actionLogRepository.findAll().get(0);
        assertEquals("File deleted: Docs/a.txt", log.getDescription());
        assertEquals(ActionType.DELETE, log.getActionType());
        var expectedDateTime = Instant.parse("2024-01-01T07:00:00Z")
                .atZone(ZoneId.of("UCT"))
                .toLocalDateTime();
        assertEquals(expectedDateTime, log.getTimestamp());
    }

    @Test
    void testGetDuplicates_SameHashesSameSize_ReturnsDuplicates() {
        // given
        var a = exampleFileInfo("Docs/a.txt", 100, 100);
        var b = exampleFileInfo("Docs/b.txt", 200, 100);
        var c = exampleFileInfo("Docs/c.txt", 100, 100);
        var d = exampleFileInfo("Docs/d.txt", 100, 100);
        var e = exampleFileInfo("Docs/e.txt", 200, 100);
        var f = exampleFileInfo("Docs/f.txt", 200, 100);

        when(fileSystemService.searchDirectory("Docs/", defaultPattern)).thenReturn(List.of(a, b, c, d, e, f));
        setupFileHashes(List.of(a, b, c, d, e, f),
                List.of("1", "2", "1", "1", "2", "3"));
        fileService.loadFromPath("Docs/", defaultPattern);

        // when
        List<List<File>> duplicates = fileService.findDuplicatedGroups();
        List<List<String>> fileNames = duplicates.stream().map(inner -> inner.stream().map(File::getName).toList()).toList();

        // then
        assertEquals(2, duplicates.size());
        assertTrue(fileNames.contains(List.of("a.txt", "c.txt", "d.txt")));
        assertTrue(fileNames.contains(List.of("b.txt", "e.txt")));
    }

    @Test
    void testArchiveFiles() throws IOException {
        // Given
        var a = exampleFileInfo("Docs/a.txt", 100, 100);
        var b = exampleFileInfo("Docs/b.txt", 200, 100);
        var c = exampleFileInfo("Docs/c.txt", 300, 100);

        when(fileSystemService.searchDirectory("Docs/", defaultPattern)).thenReturn(List.of(a, b, c));
        fileService.loadFromPath("Docs/", defaultPattern);

        List<File> docsFiles = fileService.findFilesInPath("Docs/");
        String zipPath = "Docs/test.zip";

        when(fileSystemService.openFileForRead("Docs/a.txt"))
                .thenReturn(new ByteArrayInputStream("a".getBytes()));
        when(fileSystemService.openFileForRead("Docs/b.txt"))
                .thenReturn(new ByteArrayInputStream("b".getBytes()));
        when(fileSystemService.openFileForRead("Docs/c.txt"))
                .thenReturn(new ByteArrayInputStream("c".getBytes()));

        // Use ByteArrayOutputStream to simulate the ZIP file output
        ByteArrayOutputStream zipFileOutputStream = new ByteArrayOutputStream();
        when(fileSystemService.openFileForWrite(zipPath)).thenReturn(zipFileOutputStream);

        // When
        fileService.archiveFiles(docsFiles, zipPath);

        // Then
        ByteArrayInputStream zipInputStream = new ByteArrayInputStream(zipFileOutputStream.toByteArray());
        try (ZipInputStream zipIn = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            int fileCount = 0;

            while ((entry = zipIn.getNextEntry()) != null) {
                fileCount++;
                switch (entry.getName()) {
                    case "a.txt" -> assertEquals("a", new String(zipIn.readAllBytes()));
                    case "b.txt" -> assertEquals("b", new String(zipIn.readAllBytes()));
                    case "c.txt" -> assertEquals("c", new String(zipIn.readAllBytes()));
                    default -> fail("Unexpected file in ZIP: " + entry.getName());
                }
                zipIn.closeEntry();
            }

            assertEquals(3, fileCount, "Incorrect number of files in the ZIP archive.");
        }
    }

    @Test
    void testFindVersions_FindsFilesWhoseNamesAreWithinPassedEditDistance() {
        // given
        var v1 = exampleFileInfo("Docs/ver1.txt");
        var v2 = exampleFileInfo("Docs/ver2.txt");
        var v3 = exampleFileInfo("Docs/ver3.txt");
        var v10 = exampleFileInfo("Docs/ver10.txt");
        var v_9 = exampleFileInfo("Docs/ver_9.txt");
        var hello = exampleFileInfo("Docs/hello.txt");
        var hello2 = exampleFileInfo("Docs/hello2.txt");
        var world = exampleFileInfo("Docs/world.txt");

        when(fileSystemService.searchDirectory("Docs/", defaultPattern))
                .thenReturn(List.of(v1, v2, v3, v10, v_9, hello, hello2, world));
        fileService.loadFromPath("Docs/", defaultPattern);

        // when
        List<Set<File>> versions = fileService.findVersions(3);

        // then
        assertEquals(2, versions.size());
        System.out.println(versions.stream().map(inner -> inner.stream().map(File::getName).toList()).toList());
        List<Set<String>> versionFileNames = versions.stream()
                .map(inner -> inner.stream().map(File::getName).collect(Collectors.toSet()))
                .toList();

        assertTrue(versionFileNames.contains(Set.of("ver1.txt", "ver2.txt", "ver3.txt", "ver10.txt", "ver_9.txt")));
        assertTrue(versionFileNames.contains(Set.of("hello.txt", "hello2.txt")));
    }

}