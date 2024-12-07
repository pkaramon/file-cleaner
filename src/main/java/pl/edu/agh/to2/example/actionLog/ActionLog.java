package pl.edu.agh.to2.example.actionLog;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class ActionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @Column(nullable = false)
    private ActionType type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public ActionLog() {
    }

    public ActionLog(ActionType type, String description, LocalDateTime timestamp) {
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public ActionType getActionType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
