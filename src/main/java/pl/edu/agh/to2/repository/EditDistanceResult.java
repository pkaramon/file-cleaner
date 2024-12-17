package pl.edu.agh.to2.repository;

import pl.edu.agh.to2.model.File;

public record EditDistanceResult(File first, File second, int distance) {
}
