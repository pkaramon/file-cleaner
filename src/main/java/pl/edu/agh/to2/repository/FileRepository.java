package pl.edu.agh.to2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.edu.agh.to2.model.File;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    Optional<File> findByPath(String path);

    List<File> findByPathStartingWith(String path);

    @Query("SELECT f from File f WHERE f.path LIKE :path% ORDER BY f.size DESC LIMIT :limit")
    List<File> findLargestFilesIn(String path, int limit);

    @Modifying
    @Query("DELETE FROM File f WHERE f.path = :path")
    void deleteByPath(String path);

    @Modifying
    @Query("DELETE FROM File f")
    void deleteAll();


    @Query("SELECT f FROM File f WHERE (f.hash, f.size) in " +
            "(SELECT f.hash, f.size FROM File f GROUP BY f.hash, f.size HAVING COUNT(f) > 1) " +
            "ORDER BY f.id")
    List<File> findDuplicates();

    @Query("SELECT levenshtein(f.name, g.name), f, g " +
            "FROM File f " +
            "INNER JOIN File g ON f.id < g.id " +
            "WHERE levenshtein(f.name, g.name) < :maxDistance " +
            "ORDER BY levenshtein(f.name, g.name), f.id, g.id")
    List<Object[]> _findSimilarFilesWithLevenshtein(@Param("maxDistance") int maxDistance);


    default List<EditDistanceResult> findSimilarFileNames(int maxDistance) {
        return _findSimilarFilesWithLevenshtein(maxDistance).stream()
                .map(o -> new EditDistanceResult((File) o[1], (File) o[2], (int) o[0]))
                .toList();
    }


    @Query("SELECT new pl.edu.agh.to2.repository.FileSizeStats(" +
            "COALESCE(AVG(f.size), 0), COALESCE(STDDEV_POP(f.size), 0), " +
            "COALESCE(MIN(f.size), 0), COALESCE(MAX(f.size), 0), " +
            "COALESCE(COUNT(f), 0)) FROM File f")
    FileSizeStats findFileSizeStats();

    @Query("SELECT f.size FROM File f")
    List<Long> findAllSizes();

    @Query("SELECT f.lastModified FROM File f")
    List<Long> findAllLastModifiedInMilliseconds();

    @Query(value = "SELECT " +
            "CASE " +
            "  WHEN f.name NOT LIKE '%.%' THEN '' " +
            "  ELSE split_part(f.name, '.', -1) " +
            "END AS extension, " +
            "COUNT(f.id) " +
            "FROM File f " +
            "GROUP BY " +
            "CASE " +
            "  WHEN f.name NOT LIKE '%.%' THEN '' " +
            "  ELSE split_part(f.name, '.', -1) " +
            "END",
            nativeQuery = true
    )
    List<Object[]> _findFileCountsByExtension();


    default Map<String, Long> findFileCountsByExtension() {
        return _findFileCountsByExtension().stream()
                .collect(Collectors.toMap(o -> (String) o[0], o -> (Long) o[1]));
    }
}

