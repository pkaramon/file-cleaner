package pl.edu.agh.to2.example.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    Optional<File> findByPath(String path);

    List<File> findByPathStartingWith(String path);

    default Map<String, File> getMapFromPathToFile() {
        return findAllAsStream().collect(Collectors.toMap(File::getPath, f -> f));
    }

    @Query("SELECT f from File f")
    Stream<File> findAllAsStream();

    @Query("SELECT f from File f WHERE f.path LIKE :path% ORDER BY f.size DESC LIMIT :limit")
    List<File> findLargestFilesIn(String path, int limit);

    void deleteByPath(String path);
}
