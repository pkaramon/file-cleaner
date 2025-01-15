package pl.edu.agh.to2.service;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
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
import pl.edu.agh.to2.repository.FileSizeStats;
import pl.edu.agh.to2.types.ActionType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    FileSystem fs;

    @Autowired
    @InjectMocks
    private FileService fileService;
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private ActionLogRepository actionLogRepository;
    @MockBean
    private FileHasher fileHasher;
    @MockBean
    private Clock clock;

    @BeforeEach
    void setup() throws IOException {
        fileRepository.deleteAll();
        actionLogRepository.deleteAll();

        when(fileHasher.hash(Mockito.any())).thenReturn("hash");
        when(clock.instant()).thenReturn(Instant.now());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());
        fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    void tearDown() throws IOException {
        fs.close();
    }

    List<Path> setupDirectory(Path dir, String... fileNames) throws IOException {
        return setupDirectoryWithHashes(dir, List.of(fileNames), null);
    }

    void setupDirectoryWithContents(Path dir, List<String> fileNames, List<String> contents) throws IOException {
        baseSetupDirectory(dir, fileNames, null, contents);
    }

    List<Path> setupDirectoryWithHashes(Path dir, List<String> fileNames, List<String> hashes) throws IOException {
        return baseSetupDirectory(dir, fileNames, hashes, null);
    }

    List<Path> baseSetupDirectory(Path dir,
                                  List<String> fileNames,
                                  List<String> hashes,
                                  List<String> contents) throws IOException {
        Files.createDirectory(dir);
        for (int i = 0; i < fileNames.size(); i++) {
            Path file = dir.resolve(fileNames.get(i));
            Files.createDirectories(file.getParent());
            if (contents != null) {
                Files.write(file, contents.get(i).getBytes());
            } else {
                Files.createFile(file);
            }
            if (hashes != null) {
                when(fileHasher.hash(file)).thenReturn(hashes.get(i));
            }
        }

        return fileNames.stream()
                .map(dir::resolve)
                .toList();
    }

    @Test
    void testFindLargestFiles() {
        // given
        File a = new File("a.txt", "Docs/a.txt", 100, 100, "a");
        File b = new File("b.txt", "Docs/b.txt", 200, 200, "b");
        File c = new File("c.txt", "Docs/c.txt", 300, 300, "c");
        File d = new File("notes.txt", "Desktop/notes.txt", 400, 400, "d");

        fileRepository.saveAll(List.of(a, b, c, d));

        // when
        List<File> largestFiles = fileService.findLargestFilesIn(Path.of("Docs/"), 2);

        // then
        assertEquals(
                largestFiles.stream().map(File::getName).toList(),
                List.of("c.txt", "b.txt")
        );
    }

    @Test
    void testLoadFromPath_WhenDatabaseIsEmpty_AddsAllTheFiles() throws IOException {
        // given
        Path dir = fs.getPath("Docs/");
        setupDirectory(dir, "a.txt", "b.txt");

        // when
        fileService.loadFromPath(dir, defaultPattern);

        // then
        List<File> files = fileService.findFilesInPath(dir);
        assertEquals(2, files.size());
        assertTrue(files.contains(fileRepository.findByPath("Docs/a.txt").orElseThrow()));
        assertTrue(files.contains(fileRepository.findByPath("Docs/b.txt").orElseThrow()));
    }

    @Test
    void testLoadFromPath_RespectsPassedPattern() throws IOException {
        // given
        Path dir = fs.getPath("Docs/");
        setupDirectory(dir, "a.txt", "c.doc");

        // when
        fileService.loadFromPath(dir, Pattern.compile(".*\\.txt$"));

        // then
        List<File> files = fileService.findFilesInPath(dir);
        assertEquals(1, files.size());
        assertTrue(files.contains(fileRepository.findByPath("Docs/a.txt").orElseThrow()));
    }

    @Test
    void testLoadFromPath_WhenThereAreSubdirectories_ItRecursivelyTraversesThem() throws IOException {
        // given
        Path root = fs.getPath("Root/");
        setupDirectory(root, "Desktop/file1.txt", "Docs/file2.txt", "Docs/file3.txt");
        Pattern pattern = Pattern.compile(".*\\.txt$");

        // when
        fileService.loadFromPath(root, pattern);

        // then
        List<File> result = fileService.findFilesInPath(root);
        assertEquals(3, result.size());
        assertEquals(Set.of("file1.txt", "file2.txt", "file3.txt"),
                result.stream().map(File::getName).collect(Collectors.toSet()));
    }


    @Test
    void testLoadFromPath_WhenFileIsDeletedFromDirectory_ItsAlsoDeletedFromDb() throws IOException {
        // given
        Path dir = fs.getPath("Docs/");
        setupDirectory(dir, "a.txt", "b.txt");

        fileService.loadFromPath(dir, defaultPattern);

        Files.delete(dir.resolve("a.txt"));

        // when
        fileService.loadFromPath(dir, defaultPattern);

        // then
        List<File> files = fileService.findFilesInPath(dir);
        assertEquals(1, files.size());
        assertEquals("b.txt", files.get(0).getName());
    }

    @Test
    void testLoadFromPath_ComputesHashForNewFiles() throws IOException {
        // given
        Path dir = fs.getPath("Docs/");
        Path a = setupDirectoryWithHashes(dir, List.of("a.txt"), List.of("a_hash")).get(0);

        // when
        fileService.loadFromPath(dir, defaultPattern);

        // then
        verify(fileHasher).hash(a);
        File fileA = fileRepository.findByPath("Docs/a.txt").orElseThrow();
        assertEquals("a_hash", fileA.getHash());
    }

    @Test
    void testLoadFromPath_SomeFilesHaveChanged_ItUpdatesAllFiles() throws IOException {
        // given
        Path dir = fs.getPath("Docs/");
        List<Path> files = setupDirectoryWithHashes(dir,
                List.of("a.txt", "b.txt", "c.txt", "d.txt"),
                List.of("a_hash", "b_hash", "c_hash", "d_hash")
        );

        fileService.loadFromPath(dir, defaultPattern);


        Files.setLastModifiedTime(files.get(0), FileTime.from(123, TimeUnit.SECONDS));
        Files.setLastModifiedTime(files.get(1), FileTime.from(321, TimeUnit.SECONDS));
        Files.delete(files.get(3));

        // when
        fileService.loadFromPath(dir, defaultPattern);

        // then
        assertEquals(3, fileRepository.findAll().size());
        verify(fileHasher, times(2)).hash(files.get(0));
        verify(fileHasher, times(2)).hash(files.get(1));
        verify(fileHasher, times(2)).hash(files.get(2));
        verify(fileHasher, times(1)).hash(files.get(3));

    }

    @Test
    void testDeleteFile() throws IOException {
        // given
        Path dir = fs.getPath("Docs/");
        Path a = setupDirectory(dir, "a.txt").get(0);

        when(clock.instant()).thenReturn(Instant.parse("2024-01-01T07:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UCT"));

        // when
        fileService.deleteFile(a);

        // then
        assertTrue(Files.notExists(a));
        assertTrue(fileRepository.findByPath("Docs/a.txt").isEmpty());

        var log = actionLogRepository.findAll().get(0);
        assertEquals("File deleted: Docs/a.txt", log.getDescription());
        assertEquals(ActionType.DELETE, log.getActionType());
        var expectedDateTime = Instant.parse("2024-01-01T07:00:00Z")
                .atZone(ZoneId.of("UCT"))
                .toLocalDateTime();
        assertEquals(expectedDateTime, log.getTimestamp());
    }

    @Test
    void testGetDuplicates_IfFilesHaveTheSameHashSameSize_ReturnsThem() throws IOException {
        // given
        Path dir = fs.getPath("Docs/");
        setupDirectoryWithHashes(dir,
                List.of("a.txt", "b.txt", "c.txt", "d.txt", "e.txt", "f.txt"),
                List.of("1", "2", "1", "1", "2", "3")
        );
        fileService.loadFromPath(dir, defaultPattern);

        // when
        List<List<File>> duplicates = fileService.findDuplicatedGroups();
        List<List<String>> fileNames = duplicates
                .stream()
                .map(inner -> inner.stream().map(File::getName).toList())
                .toList();

        // then
        assertEquals(2, duplicates.size());
        assertTrue(fileNames.contains(List.of("a.txt", "c.txt", "d.txt")));
        assertTrue(fileNames.contains(List.of("b.txt", "e.txt")));
    }

    @Test
    void testArchiveFiles() throws IOException {
        // given
        var dir = fs.getPath("Docs/");
        setupDirectoryWithContents(
                dir,
                List.of("a.txt", "b.txt", "c.txt", "nested/a.txt", "a(1).txt"),
                List.of("a", "b", "c", "a", "a")
        );

        Files.createDirectory(fs.getPath("Archives/"));
        Path zipPath = fs.getPath("Archives/test.zip");

        fileService.loadFromPath(dir, defaultPattern);
        var docsFiles = fileService.findFilesInPath(dir);

        // when
        fileService.archiveFiles(docsFiles, zipPath);

        // then
        assertTrue(Files.exists(zipPath));
        try (InputStream is = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(is)
        ) {
            ZipEntry entry;
            int fileCount = 0;
            while ((entry = zis.getNextEntry()) != null) {
                fileCount++;
                switch (entry.getName()) {
                    case "a.txt", "a(1).txt", "a(2).txt" -> assertEquals("a", new String(zis.readAllBytes()));
                    case "b.txt" -> assertEquals("b", new String(zis.readAllBytes()));
                    case "c.txt" -> assertEquals("c", new String(zis.readAllBytes()));
                    default -> fail("Unexpected file in ZIP: " + entry.getName());
                }
                zis.closeEntry();
            }
            assertEquals(5, fileCount, "Incorrect number of files in the ZIP archive.");
        }
    }


    @Test
    void testArchivesFiles_AddsActionLog() throws IOException {
        // given
        var dir = fs.getPath("Docs/");
        setupDirectoryWithContents(dir, List.of("a.txt"), List.of("a"));
        fileService.loadFromPath(dir, defaultPattern);
        var fileA = fileRepository.findByPath("Docs/a.txt").orElseThrow();
        Files.createDirectory(fs.getPath("Archives/"));
        Path zipPath = fs.getPath("Archives/test.zip");

        // when
        fileService.archiveFiles(List.of(fileA), zipPath);

        // then
        var log = actionLogRepository.findAll().get(0);
        assertEquals("Files archived to: Archives/test.zip", log.getDescription());
        assertEquals(ActionType.ARCHIVE, log.getActionType());
    }

    @Test
    void testFindVersions_FindsFilesWhoseNamesAreWithinPassedEditDistance() throws IOException {
        // given
        var dir = fs.getPath("Docs/");
        setupDirectory(dir, "ver1.txt", "ver2.txt", "ver3.txt", "ver10.txt",
                "ver_9.txt", "hello.txt", "hello2.txt", "world.txt");

        fileService.loadFromPath(dir, defaultPattern);

        // when
        List<List<File>> versions = fileService.findVersions(3);

        // then
        assertEquals(2, versions.size());
        List<Set<String>> versionFileNames = versions.stream()
                .map(inner -> inner.stream().map(File::getName).collect(Collectors.toSet()))
                .toList();

        assertTrue(versionFileNames.contains(Set.of("ver1.txt", "ver2.txt", "ver3.txt", "ver10.txt", "ver_9.txt")));
        assertTrue(versionFileNames.contains(Set.of("hello.txt", "hello2.txt")));
    }


    @Test
    void testGetFileSizeStats_WhenNoFiles_ReturnsEmpty() {
        // when
        var stats = fileService.getFileSizeStats();

        // then
        assertTrue(stats.isEmpty());
    }

    @Test
    void testGetFileSizeStats_Average_Std_Min_Max_Count() throws IOException {
        // given
        var dir = fs.getPath("Docs/");
        setupDirectoryWithContents(
                dir,
                List.of("a.txt", "b.txt", "c.txt"),
                List.of("a".repeat(100), "b".repeat(200), "c".repeat(300))
        );
        fileService.loadFromPath(dir, defaultPattern);

        // when
        FileSizeStats stats = fileService.getFileSizeStats().orElseThrow();

        // then
        assertEquals(81.65, stats.std(), 0.01);
        assertEquals(200, stats.average(), 1e-6);
        assertEquals(100, stats.min(), 1e-6);
        assertEquals(300, stats.max(), 1e-6);
        assertEquals(3, stats.count());
    }

}