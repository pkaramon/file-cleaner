package pl.edu.agh.to2.example.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    @Query("SELECT f from File f ORDER BY f.size DESC LIMIT :limit")
    List<File> findLargestFiles(int limit);

    Optional<File> findByPath(String path);
}
