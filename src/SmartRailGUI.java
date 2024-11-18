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

            Scene scene = new Scene(root, 800, 600);
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

    private void startRenderLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawRailNetwork();
            }
        };
        timer.start();
    }

    private void createCanvas() {
        railCanvas = new Canvas(600, 500);
        canvasContainer.getChildren().add(railCanvas);

        railCanvas.widthProperty().bind(canvasContainer.widthProperty());
        railCanvas.heightProperty().bind(canvasContainer.heightProperty());
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(200);

        Label sourceLabel = new Label("Source Station:");
        sourceStationBox = new ComboBox<>();
        sourceStationBox.setMaxWidth(Double.MAX_VALUE);

        Label destLabel = new Label("Destination Station:");
        destStationBox = new ComboBox<>();
        destStationBox.setMaxWidth(Double.MAX_VALUE);

        addTrainButton = new Button("Add Train");
        addTrainButton.setMaxWidth(Double.MAX_VALUE);
        addTrainButton.setOnAction(e -> addNewTrain());

        panel.getChildren().addAll(
                sourceLabel, sourceStationBox,
                destLabel, destStationBox,
                new Separator(),
                addTrainButton
        );

        return panel;
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

        for (int i = 0; i <= 10; i++) {
            double x = transformX(i);
            double y = transformY(i);
            gc.strokeLine(x, 0, x, railCanvas.getHeight());
            gc.strokeLine(0, y, railCanvas.getWidth(), y);
        }
    }

    private void drawTrack(Track track, GraphicsContext gc) {
        gc.setStroke(track.isLocked() ? Color.RED : Color.BLACK);
        gc.setLineWidth(2);
        double x1 = transformX(track.getStartX());
        double y1 = transformY(track.getStartY());
        double x2 = transformX(track.getEndX());
        double y2 = transformY(track.getEndY());
        gc.strokeLine(x1, y1, x2, y2);

        // Draw segments
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
        gc.setFill(station.isLocked() ? Color.RED : Color.GREEN);
        double x = transformX(station.getX());
        double y = transformY(station.getY());
        gc.fillRect(x - 15, y - 15, 30, 30);

        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(x - 15, y - 15, 30, 30);

        gc.setFill(Color.WHITE);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(station.getName(), x, y + 5);
    }

    private void drawTrain(Train train, GraphicsContext gc) {
        Color trainColor;
        if (train.getStatus() != null) {
            switch (train.getStatus()) {
                case IDLE: trainColor = Color.GRAY; break;
                case SEEKING_PATH: trainColor = Color.YELLOW; break;
                case LOCKING_PATH: trainColor = Color.PURPLE; break;
                case MOVING: trainColor = Color.GREEN; break;
                case EXITED: trainColor = Color.TRANSPARENT; break;
                default: trainColor = Color.BLACK;
            }
        } else {
            trainColor = Color.BLACK;
        }

        double x = transformX(train.getX());
        double y = transformY(train.getY());

        gc.setFill(trainColor);
        if (trainColor != Color.TRANSPARENT) {
            gc.fillOval(x - 10, y - 10, 20, 20);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            gc.strokeOval(x - 10, y - 10, 20, 20);
        }
    }

    private double transformX(double x) {
        return x * SCALE_FACTOR + CANVAS_PADDING;
    }

    private double transformY(double y) {
        return y * SCALE_FACTOR + CANVAS_PADDING;
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

        // Check if there are any other trains currently moving
        for (Train t : trains) {
            if (t.getStatus() == Train.Status.MOVING) {
                isTrainMoving = true;
                sourceStationBox.setDisable(true);
                destStationBox.setDisable(true);
                addTrainButton.setDisable(true);
                return;
            }
        }

        // If no other trains are moving, set the destination for the new train
        train.setDestination(dest);
        isTrainMoving = true;
    }

    private void loadConfiguration(String filename) {
        try {
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

        } catch (Exception e) {
            showError("Configuration Error", "Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void startSimulation() {
        for (Component component : components) {
            Thread thread = new Thread(component);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SmartRailGUI <config-file>");
            System.exit(1);
        }
        launch(args);
    }
}