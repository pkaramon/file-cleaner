package pl.edu.agh.to2.example.file;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class FileServiceTest {
    @Autowired
    private FileService fileService;

    @Autowired
    private FileRepository fileRepository;

    @BeforeEach
    void setupDb() {
        fileRepository.deleteAll();

        File a = new File("a.txt", "Documents/a.txt", 100, 100);
        File b = new File("b.txt", "Documents/b.txt", 200, 200);
        File c = new File("c.txt", "Documents/c.txt", 300, 300);

        fileRepository.saveAll(List.of(a, b, c));
    }

    @Test
    void testFindLargestFiles() {
        List<File> largestFiles = fileService.findLargestFiles(2);

        assertEquals(
                largestFiles.stream().map(File::getName).toList(),
                List.of("c.txt", "b.txt")
        );
    }


}