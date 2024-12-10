package pl.edu.agh.to2.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.service.ActionLogService;

import java.time.format.DateTimeFormatter;

@Component
public class ActionLogListViewController {
    private final ActionLogService actionLogService;

    @FXML
    private TableView<ActionLogRow> tableView;
    @FXML
    private TableColumn<ActionLogRow, String> dateColumn;
    @FXML
    private TableColumn<ActionLogRow, String> descColumn;
    @FXML
    private TableColumn<ActionLogRow, String> typeColumn;

    public ActionLogListViewController(ActionLogService actionLogService) {
        this.actionLogService = actionLogService;
    }


    @FXML
    private void initialize() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        descColumn.setCellValueFactory(new PropertyValueFactory<>("desc"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
    }

    public void show() {
        var logs = actionLogService.getLogs();
        tableView.getItems().clear();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        tableView.getItems().addAll(logs.stream()
                .map(log -> new ActionLogRow(
                        log.getTimestamp().format(formatter),
                        log.getDescription(),
                        log.getActionType().toString()))
                .toList());
    }

    public static class ActionLogRow {
        private final String date;
        private final String desc;
        private final String type;

        public ActionLogRow(String date, String desc, String type) {
            this.date = date;
            this.desc = desc;
            this.type = type;
        }

        public String getDate() {
            return date;
        }

        public String getDesc() {
            return desc;
        }

        public String getType() {
            return type;
        }
    }
}
