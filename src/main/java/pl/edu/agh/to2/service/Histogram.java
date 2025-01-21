package pl.edu.agh.to2.service;

import java.util.List;

public record Histogram(
        long min,
        long max,
        List<Long> buckets
) {

    public long bucketSize() {
        return (max - min + 1) / buckets.size();
    }
}
