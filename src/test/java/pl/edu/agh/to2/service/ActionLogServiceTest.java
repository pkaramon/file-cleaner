package pl.edu.agh.to2.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import pl.edu.agh.to2.model.ActionLog;
import pl.edu.agh.to2.repository.ActionLogRepository;
import pl.edu.agh.to2.types.ActionType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
class ActionLogServiceTest {
    @Autowired
    private ActionLogService actionLogService;
    @Autowired
    private ActionLogRepository actionLogRepository;

    @Test
    void testGetLogs_ReturnsNewestFirst() {
        var a = new ActionLog(
                ActionType.DELETE,
                "a task",
                LocalDateTime.of(2022, 1, 1, 1, 1, 1)
        );
        var b = new ActionLog(
                ActionType.DELETE,
                "b task",
                LocalDateTime.of(2021, 1, 1, 1, 1, 1)
        );
        actionLogRepository.saveAllAndFlush(List.of(b, a));

        var logs = actionLogService.getLogs();

        assertEquals(2, logs.size());
        assertEquals("a task", logs.get(0).getDescription());
        assertEquals("b task", logs.get(1).getDescription());
    }
}