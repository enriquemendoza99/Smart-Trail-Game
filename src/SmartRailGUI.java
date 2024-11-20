import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.canvas.*;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.scene.control.Alert.AlertType;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.scene.text.TextAlignment;
import java.util.*;
import java.io.*;

public class SmartRailGUI extends Application implements TrainGUI {
    private Canvas railCanvas;
    private List<Component> components = new ArrayList<>();
    private List<Train> trains = new ArrayList<>();
    private List<Station> stations = new ArrayList<>();
    private ComboBox<Station> sourceStationBox;
    private ComboBox<Station> destStationBox;
    private Button addTrainButton;
    private static final double SCALE_FACTOR = 50.0;
    private static final double CANVAS_PADDING = 50.0;
    private Pane canvasContainer;
    private boolean isTrainMoving = false;

    @Override
    public void start(Stage primaryStage) {
        try {
            BorderPane root = new BorderPane();

            // Create control panel
            VBox controlPanel = createControlPanel();
            root.setRight(controlPanel);

            // Create canvas container
            canvasContainer = new Pane();
            canvasContainer.setStyle("-fx-background-color: white;");
            root.setCenter(canvasContainer);

            // Create canvas
            createCanvas();

            Scene scene = new Scene(root, 1000, 800);
            primaryStage.setTitle("SmartRail Simulator");
            primaryStage.setScene(scene);

            // Load configuration
            List<String> args = getParameters().getRaw();
            if (args.isEmpty()) {
                showError("Configuration Error", "No configuration file specified");
                return;
            }

            String configFile = args.get(0);
            loadConfiguration(configFile);

            primaryStage.show();

            // Start simulation threads
            startSimulation();

            // Start render loop
            startRenderLoop();

        } catch (Exception e) {
            showError("Error", "Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createCanvas() {
        railCanvas = new Canvas(900, 700);
        canvasContainer.getChildren().add(railCanvas);

        railCanvas.widthProperty().bind(canvasContainer.widthProperty());
        railCanvas.heightProperty().bind(canvasContainer.heightProperty());
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(200);

        // Create station selection controls
        Label sourceLabel = new Label("Source Station:");
        sourceStationBox = new ComboBox<>();
        sourceStationBox.setMaxWidth(Double.MAX_VALUE);

        Label destLabel = new Label("Destination Station:");
        destStationBox = new ComboBox<>();
        destStationBox.setMaxWidth(Double.MAX_VALUE);

        addTrainButton = new Button("Add Train");
        addTrainButton.setMaxWidth(Double.MAX_VALUE);
        addTrainButton.setOnAction(e -> addNewTrain());

        // Create legend
        TitledPane legend = createLegend();

        panel.getChildren().addAll(
                sourceLabel,
                sourceStationBox,
                destLabel,
                destStationBox,
                new Separator(),
                addTrainButton,
                new Separator(),
                legend
        );

        return panel;
    }

    private TitledPane createLegend() {
        VBox legendContent = new VBox(5);
        legendContent.setPadding(new Insets(5));

        Label stationLabel = new Label("Station (Green/Red when locked)");
        Label switchLabel = new Label("Switch (Orange/Red when locked)");
        Label trackLabel = new Label("Track (Black/Red when locked)");
        Label trainIdleLabel = new Label("Train Idle (Gray)");
        Label trainSeekingLabel = new Label("Train Seeking Path (Yellow)");
        Label trainLockingLabel = new Label("Train Locking Path (Purple)");
        Label trainMovingLabel = new Label("Train Moving (Green)");

        legendContent.getChildren().addAll(
                stationLabel,
                switchLabel,
                trackLabel,
                trainIdleLabel,
                trainSeekingLabel,
                trainLockingLabel,
                trainMovingLabel
        );

        return new TitledPane("Legend", legendContent);
    }

    private void startRenderLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawRailNetwork();
            }
        };
        timer.start();
    }

    private void drawRailNetwork() {
        if (railCanvas == null) return;

        GraphicsContext gc = railCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, railCanvas.getWidth(), railCanvas.getHeight());

        // Draw grid
        drawGrid(gc);

        // Draw tracks
        for (Component component : components) {
            if (component instanceof Track) {
                drawTrack((Track)component, gc);
            }
        }

        // Draw switches
        for (Component component : components) {
            if (component instanceof Switch) {
                drawSwitch((Switch)component, gc);
            }
        }

        // Draw stations
        for (Station station : stations) {
            drawStation(station, gc);
        }

        // Draw trains
        for (Train train : trains) {
            drawTrain(train, gc);
        }
    }

    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(0.5);

        double maxX = railCanvas.getWidth();
        double maxY = railCanvas.getHeight();

        // Draw vertical lines
        for (int i = 0; i <= 20; i++) {
            double x = transformX(i);
            if (x >= 0 && x <= maxX) {
                gc.strokeLine(x, 0, x, maxY);
            }
        }

        // Draw horizontal lines
        for (int i = 0; i <= 20; i++) {
            double y = transformY(i);
            if (y >= 0 && y <= maxY) {
                gc.strokeLine(0, y, maxX, y);
            }
        }
    }

    private void drawSwitch(Switch sw, GraphicsContext gc) {
        double x = transformX(sw.getX());
        double y = transformY(sw.getY());

        gc.setFill(sw.isLocked() ? Color.RED : Color.ORANGE);
        gc.fillOval(x - 8, y - 8, 16, 16);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeOval(x - 8, y - 8, 16, 16);
    }

    private void drawTrack(Track track, GraphicsContext gc) {
        gc.setStroke(track.isLocked() ? Color.RED : Color.BLACK);
        gc.setLineWidth(2);

        double x1 = transformX(track.getStartX());
        double y1 = transformY(track.getStartY());
        double x2 = transformX(track.getEndX());
        double y2 = transformY(track.getEndY());

        gc.strokeLine(x1, y1, x2, y2);

        // Draw segment markers
        if (track.getSegments() > 1) {
            gc.setFill(Color.BLACK);
            for (int i = 1; i < track.getSegments(); i++) {
                double progress = (double) i / track.getSegments();
                double x = x1 + (x2 - x1) * progress;
                double y = y1 + (y2 - y1) * progress;
                gc.fillOval(x - 2, y - 2, 4, 4);
            }
        }
    }

    private void drawStation(Station station, GraphicsContext gc) {
        double x = transformX(station.getX());
        double y = transformY(station.getY());

        gc.setFill(station.isLocked() ? Color.RED : Color.GREEN);
        gc.fillRect(x - 15, y - 15, 30, 30);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(x - 15, y - 15, 30, 30);

        gc.setFill(Color.BLACK);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(station.getName(), x, y + 5);
    }

    private void drawTrain(Train train, GraphicsContext gc) {
        Color trainColor = switch (train.getStatus()) {
            case IDLE -> Color.GRAY;
            case SEEKING_PATH -> Color.YELLOW;
            case LOCKING_PATH -> Color.PURPLE;
            case MOVING -> Color.GREEN;
            case EXITED -> Color.TRANSPARENT;
        };

        if (trainColor != Color.TRANSPARENT) {
            double x = transformX(train.getX());
            double y = transformY(train.getY());

            gc.setFill(trainColor);
            gc.fillOval(x - 12, y - 12, 24, 24);

            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeOval(x - 12, y - 12, 24, 24);
        }
    }

    private double transformX(double x) {
        return x * SCALE_FACTOR + CANVAS_PADDING;
    }

    private double transformY(double y) {
        return y * SCALE_FACTOR + CANVAS_PADDING;
    }

    private void loadConfiguration(String filename) throws IOException {
        ConfigurationLoader loader = new ConfigurationLoader();
        RailSystem system = loader.loadConfiguration(filename);

        components.addAll(system.getComponents());
        stations.addAll(system.getStations());

        sourceStationBox.getItems().addAll(stations);
        destStationBox.getItems().addAll(stations);

        if (!stations.isEmpty()) {
            sourceStationBox.setValue(stations.get(0));
            if (stations.size() > 1) {
                destStationBox.setValue(stations.get(1));
            }
        }
    }

    private void addNewTrain() {
        Station source = sourceStationBox.getValue();
        Station dest = destStationBox.getValue();

        if (source == null || dest == null) {
            showError("Selection Error", "Please select both source and destination stations");
            return;
        }

        if (source == dest) {
            showError("Selection Error", "Source and destination stations must be different");
            return;
        }

        Train train = new Train(source.getX(), source.getY(), this);
        train.setInitialLocation(source);
        trains.add(train);

        Thread trainThread = new Thread(train);
        trainThread.setDaemon(true);
        trainThread.start();

        if (trains.stream().anyMatch(t -> t.getStatus() == Train.Status.MOVING)) {
            isTrainMoving = true;
            sourceStationBox.setDisable(true);
            destStationBox.setDisable(true);
            addTrainButton.setDisable(true);
        } else {
            train.setDestination(dest);
            isTrainMoving = true;
            sourceStationBox.setDisable(true);
            destStationBox.setDisable(true);
            addTrainButton.setDisable(true);
        }
    }

    private void startSimulation() {
        for (Component component : components) {
            Thread thread = new Thread(component);
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // TrainGUI Interface Implementation
    @Override
    public void updateTrain(Train train) {
        Platform.runLater(() -> {
            if (train.getStatus() != Train.Status.EXITED) {
                drawRailNetwork();
            } else {
                isTrainMoving = false;
                sourceStationBox.setDisable(false);
                destStationBox.setDisable(false);
                addTrainButton.setDisable(false);
            }
        });
    }

    @Override
    public void showPathError(Train train) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Path Finding Error");
            alert.setHeaderText(null);
            alert.setContentText("Train could not find a path to the destination.");
            alert.showAndWait();
            removeTrainFromSystem(train);
        });
    }

    @Override
    public void showLockError(Train train) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Lock Error");
            alert.setHeaderText(null);
            alert.setContentText("Train could not secure the path to the destination.");
            alert.showAndWait();
            removeTrainFromSystem(train);
        });
    }

    @Override
    public void removeTrain(Train train) {
        Platform.runLater(() -> {
            trains.remove(train);
            drawRailNetwork();
        });
    }

    @Override
    public void enableOtherTrains() {
        Platform.runLater(() -> {
            sourceStationBox.setDisable(false);
            destStationBox.setDisable(false);
            addTrainButton.setDisable(false);
            isTrainMoving = false;
        });
    }

    @Override
    public void removeTrainFromSystem(Train train) {
        Platform.runLater(() -> {
            trains.remove(train);
            enableOtherTrains();
            drawRailNetwork();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}