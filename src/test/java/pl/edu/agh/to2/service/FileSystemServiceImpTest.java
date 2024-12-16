package pl.edu.agh.to2.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemServiceImpTest {
    private Path tempDir;
    private FileSystemServiceImp fileSystemService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("testDir");
        fileSystemService = new FileSystemServiceImp();
    }

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> paths = Files.walk(tempDir).sorted(Comparator.reverseOrder())) {
            paths.map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    void testSearchDirectoryWithMatchingPattern() throws IOException {
        // given
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createFile(tempDir.resolve("file3.doc"));

        Pattern pattern = Pattern.compile(".*\\.txt$");

        // when
        Collection<FileInfo> result = fileSystemService.searchDirectory(tempDir.toString(), pattern);

        // then
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(f -> f.path().endsWith("file1.txt")));
        assertTrue(result.stream().anyMatch(f -> f.path().endsWith("file2.txt")));
    }

    @Test
    void testSearchDirectoryWithNoMatches() throws IOException {
        // given
        Files.createFile(tempDir.resolve("file1.doc"));
        Files.createFile(tempDir.resolve("file2.doc"));

        Pattern pattern = Pattern.compile(".*\\.txt$");

        // when
        Collection<FileInfo> result = fileSystemService.searchDirectory(tempDir.toString(), pattern);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchDirectoryWithSubdirectories() throws IOException {
        // given
        Path subDir1 = Files.createDirectory(tempDir.resolve("subdir1"));
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(subDir1.resolve("file2.txt"));

        Path subDir2 = Files.createDirectory(tempDir.resolve("subdir2"));
        Files.createFile(subDir2.resolve("file3.txt"));

        // Pattern to search for .txt files
        Pattern pattern = Pattern.compile(".*\\.txt$");

        // when
        Collection<FileInfo> result = fileSystemService.searchDirectory(tempDir.toString(), pattern);

        // then
        assertEquals(3, result.size());
        assertTrue(result.stream().anyMatch(f -> f.path().endsWith("file1.txt")));
        assertTrue(result.stream().anyMatch(f -> f.path().endsWith("file2.txt")));
        assertTrue(result.stream().anyMatch(f -> f.path().endsWith("file3.txt")));
    }

    @Test
    void testSearchDirectoryWithNonExistentDirectory() {
        Collection<FileInfo> result = fileSystemService.searchDirectory("nonExistentDir", null);

        assertTrue(result.isEmpty());
    }
}
