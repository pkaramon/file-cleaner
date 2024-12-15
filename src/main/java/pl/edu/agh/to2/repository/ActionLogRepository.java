package pl.edu.agh.to2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.edu.agh.to2.model.ActionLog;

import java.util.List;


public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {
    List<ActionLog> findAllByOrderByTimestampDesc();
}
