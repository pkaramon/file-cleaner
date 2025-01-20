package pl.edu.agh.to2.gui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import org.springframework.stereotype.Component;
import pl.edu.agh.to2.gui.utils.TaskExecutor;
import pl.edu.agh.to2.repository.FileSizeStats;
import pl.edu.agh.to2.service.FileService;
import pl.edu.agh.to2.service.Histogram;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Component
public class ReportsViewController {
    private final FileService fileService;
    private final Timeline debounceTimeline = new Timeline();
    private TaskExecutor taskExecutor;
    @FXML
    private Label nothingFoundLabel;

    @FXML
    private Slider numberOfBucketsSlider;

    @FXML
    private Label sliderLabel;

    @FXML
    private Pane rootPane;

    @FXML
    private Label averageLabel;

    @FXML
    private Label stdLabel;

    @FXML
    private Label minLabel;

    @FXML
    private Label maxLabel;

    @FXML
    private Label countLabel;

    @FXML
    private BarChart<String, Number> sizeHist;

    @FXML
    private BarChart<String, Number> lastModifiedHist;


    public ReportsViewController(FileService fileService) {
        this.fileService = fileService;
    }


    @FXML
    private void initialize() {
        taskExecutor = new TaskExecutor(rootPane);
        initializeSlider();
        Stream.of(sizeHist, lastModifiedHist).forEach(hist -> {
            hist.setAnimated(false);
            hist.setMaxHeight(400);
            hist.layout();
        });
    }

    private void initializeSlider() {
        numberOfBucketsSlider.setMin(1);
        numberOfBucketsSlider.setMax(25);
        numberOfBucketsSlider.setValue(10);
        numberOfBucketsSlider.setShowTickMarks(true);
        numberOfBucketsSlider.setShowTickLabels(true);
        numberOfBucketsSlider.setMajorTickUnit(10);
        numberOfBucketsSlider.setBlockIncrement(1);
        sliderLabel.textProperty().bind(numberOfBucketsSlider.valueProperty().asString("Number of buckets: %.0f"));
        numberOfBucketsSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            // We only want to update the histogram when the user has stopped changing the slider.
            // Changing the histogram immediately produces visual glitches and is resource intensive.
            debounceTimeline.stop();
            debounceTimeline.getKeyFrames().setAll(
                    new KeyFrame(Duration.millis(300), event -> {
                        taskExecutor.run(() -> fileService.getFileSizeHistogram(newValue.intValue()), this::displayFileSizeHistogram);
                        taskExecutor.run(() -> fileService.getLastModifiedHistogram(newValue.intValue()), this::displayLastModifiedHistogram);
                    })
            );
            debounceTimeline.playFromStart();
        });
    }


    public void show() {
        taskExecutor.run(fileService::getFileSizeStats, this::displayStats);
//        taskExecutor.run(() -> fileService.getFileSizeHistogram((int) numberOfBucketsSlider.getValue()), this::displayFileSizeHistogram);

    }


    private void displayStats(Optional<FileSizeStats> fileSizeStats) {
        if (fileSizeStats.isPresent()) {
            var stats = fileSizeStats.get();
            nothingFoundLabel.setVisible(false);

            countLabel.setText("Number of files: " + stats.count());
            averageLabel.setText("Average file size: " + bytesToHumanReadable((long) stats.average()));
            maxLabel.setText("Max file size: " + bytesToHumanReadable(stats.max()));
            minLabel.setText("Min file size: " + bytesToHumanReadable(stats.min()));
            stdLabel.setText("File size sd: " + bytesToHumanReadable((long) stats.std()));
        } else {
            nothingFoundLabel.setVisible(true);
        }
    }


    private String bytesToHumanReadable(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String units = "KMGTPE";
        char unit = units.charAt(exp - 1);
        return "%.1f %sB".formatted(bytes / Math.pow(1024, exp), unit);
    }


    private void displayFileSizeHistogram(Optional<Histogram> hist) {
        displayHistogram(hist, sizeHist, "File Size Histogram", this::bytesToHumanReadable);
    }

    private void displayLastModifiedHistogram(Optional<Histogram> hist) {
        displayHistogram(hist, lastModifiedHist, "Last Modified Histogram", this::millisToDate);
    }


    private void displayHistogram(Optional<Histogram> hist,
                                  BarChart<String, Number> chart,
                                  String title,
                                  Function<Long, String> rangeFormat
    ) {
        if (hist.isEmpty()) {
            return;
        }

        Histogram histogram = hist.get();
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Range");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Frequency");

        chart.setTitle(title);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        long min = histogram.min();
        long bucketSize = histogram.bucketSize();
        List<Long> buckets = histogram.buckets();

        for (int i = 0; i < buckets.size(); i++) {
            long rangeStart = min + i * bucketSize;
            long rangeEnd = rangeStart + bucketSize - 1;
            String rangeLabel = rangeFormat.apply(rangeStart) + "-" + rangeFormat.apply(rangeEnd);
            series.getData().add(new XYChart.Data<>(rangeLabel, buckets.get(i)));
        }

        chart.getData().clear();
        chart.getData().add(series);
        chart.layout();
    }

    private String millisToDate(long millis) {
        Date date = new Date(millis);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }
}
