package pl.edu.agh.to2.service;

import org.springframework.stereotype.Service;
import pl.edu.agh.to2.model.ActionLog;
import pl.edu.agh.to2.repository.ActionLogRepository;

import java.util.List;

@Service
public class ActionLogService {
    private final ActionLogRepository actionLogRepository;

    public ActionLogService(ActionLogRepository actionLogRepository) {
        this.actionLogRepository = actionLogRepository;
    }

    public List<ActionLog> getLogs() {
        return actionLogRepository.findAllByOrderByTimestampDesc();
    }
}
