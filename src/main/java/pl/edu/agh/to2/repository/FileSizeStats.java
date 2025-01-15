package pl.edu.agh.to2.repository;

public record FileSizeStats(
        double average,
        double std,
        long min,
        long max,
        long count
) {
}
