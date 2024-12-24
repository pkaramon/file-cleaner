package pl.edu.agh.to2.service;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SHA256FileHasherTest {
    private final SHA256FileHasher fileHasher = new SHA256FileHasher();
    private Path tempDir;
    private FileSystem fs;

    @BeforeEach
    void setUp() {
        fs = Jimfs.newFileSystem(Configuration.unix());
        tempDir = fs.getPath("testDir");
    }

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> paths = Files.walk(tempDir).sorted(Comparator.reverseOrder())) {
            List<Path> pathsList = paths.toList();
            for (Path path : pathsList) {
                Files.deleteIfExists(path);
            }
        }
        fs.close();
    }

    @Test
    void testHash_HashesAreTheSameIfTheContentIsTheSame() throws IOException {
        // given
        var file1 = tempDir.resolve("file1.txt");
        var file2 = tempDir.resolve("file2.txt");
        var bytes = new byte[20_000];
        Random random = new Random();
        random.nextBytes(bytes);
        Files.write(file1, bytes);
        Files.write(file2, bytes);

        // when
        var hash1 = fileHasher.hash(file1);
        var hash2 = fileHasher.hash(file2);

        // then
        assertEquals(hash1, hash2);
    }

    @Test
    void testHash_CreatesHashesOfEqualLengthButDifferentForDifferentFiles() throws IOException {
        // given
        var file1 = Files.createFile(tempDir.resolve("file1.txt"));
        var file2 = Files.createFile(tempDir.resolve("file2.txt"));
        Files.writeString(file1, "content1");
        Files.writeString(file2, "content2");

        // when
        var hash1 = fileHasher.hash(file1);
        var hash2 = fileHasher.hash(file2);

        // then
        assertEquals(64, hash1.length());
        assertEquals(hash1.length(), hash2.length());
        assertNotEquals(hash1, hash2);
    }

}
