package pl.edu.agh.to2.example.actionLog;

import org.springframework.data.jpa.repository.JpaRepository;


public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {
}
