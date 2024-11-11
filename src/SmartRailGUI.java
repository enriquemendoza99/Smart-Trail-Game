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
import java.util.*;
import java.io.*;

public class SmartRailGUI extends Application implements TrainGUI {
    private Canvas railCanvas;
    private List<Component> components = new ArrayList<>();
    private List<Train> trains = new ArrayList<>();
    private List<Station> stations = new ArrayList<>();
    private ComboBox<Station> sourceStationBox;
    private ComboBox<Station> destStationBox;
    private static final double SCALE_FACTOR = 50.0;
    private static final double CANVAS_PADDING = 50.0;
    private Pane canvasContainer;

    @Override
    public void start(Stage primaryStage) {
        try {
            BorderPane root = new BorderPane();

            // Create control panel
            VBox controlPanel = createControlPanel();
            root.setRight(controlPanel);

            // Create canvas container
            canvasContainer = new Pane();
            root.setCenter(canvasContainer);

            // Create canvas for rail network
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

        } catch (Exception e) {
            showError("Error", "Failed to start application: " + e.getMessage());
        }
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

        Button addTrainButton = new Button("Add Train");
        addTrainButton.setMaxWidth(Double.MAX_VALUE);
        addTrainButton.setOnAction(e -> addNewTrain());

        panel.getChildren().addAll(
                sourceLabel,
                sourceStationBox,
                destLabel,
                destStationBox,
                new Separator(),
                addTrainButton
        );

        return panel;
    }

    private void createCanvas() {
        railCanvas = new Canvas(600, 500);
        canvasContainer.getChildren().add(railCanvas);

        // Bind canvas size to container
        railCanvas.widthProperty().bind(canvasContainer.widthProperty());
        railCanvas.heightProperty().bind(canvasContainer.heightProperty());

        // Redraw when canvas is resized
        railCanvas.widthProperty().addListener((obs, old, newVal) -> drawRailNetwork());
        railCanvas.heightProperty().addListener((obs, old, newVal) -> drawRailNetwork());
    }

    private void loadConfiguration(String filename) {
        try {
            ConfigurationLoader loader = new ConfigurationLoader();
            RailSystem system = loader.loadConfiguration(filename);
            components.addAll(system.getComponents());
            stations.addAll(system.getStations());

            // Update station selection boxes
            sourceStationBox.getItems().addAll(stations);
            destStationBox.getItems().addAll(stations);

            if (!stations.isEmpty()) {
                sourceStationBox.setValue(stations.get(0));
                if (stations.size() > 1) {
                    destStationBox.setValue(stations.get(1));
                }
            }

            drawRailNetwork();

        } catch (Exception e) {
            showError("Configuration Error", "Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawRailNetwork() {
        if (railCanvas == null) return;

        GraphicsContext gc = railCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, railCanvas.getWidth(), railCanvas.getHeight());

        // Set background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, railCanvas.getWidth(), railCanvas.getHeight());

        // Draw grid (optional)
        drawGrid(gc);

        // Draw components
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

        // Draw lights
        for (Component component : components) {
            if (component instanceof Light) {
                drawLight((Light)component, gc);
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

        // Draw vertical lines
        for (int x = 0; x <= 10; x++) {
            double screenX = transformX(x);
            gc.strokeLine(screenX, 0, screenX, railCanvas.getHeight());
        }

        // Draw horizontal lines
        for (int y = 0; y <= 10; y++) {
            double screenY = transformY(y);
            gc.strokeLine(0, screenY, railCanvas.getWidth(), screenY);
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

        // Draw track segments if specified
        if (track.getSegments() > 1) {
            double dx = (x2 - x1) / track.getSegments();
            double dy = (y2 - y1) / track.getSegments();
            for (int i = 1; i < track.getSegments(); i++) {
                double x = x1 + dx * i;
                double y = y1 + dy * i;
                gc.setFill(Color.BLACK);
                gc.fillOval(x - 2, y - 2, 4, 4);
            }
        }
    }

    private void drawSwitch(Switch sw, GraphicsContext gc) {
        gc.setFill(sw.isLocked() ? Color.RED : Color.BLUE);
        double x = transformX(sw.getX());
        double y = transformY(sw.getY());
        gc.fillOval(x - 5, y - 5, 10, 10);

        // Draw switch position indicator
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2);
        if (sw.isMainPosition()) {
            gc.strokeLine(x, y - 5, x, y + 5);
        } else {
            gc.strokeLine(x - 5, y, x + 5, y);
        }
    }

    private void drawLight(Light light, GraphicsContext gc) {
        Color lightColor = light.getState() == Light.State.RED ? Color.RED : Color.GREEN;
        gc.setFill(lightColor);
        double x = transformX(light.getX());
        double y = transformY(light.getY());
        gc.fillOval(x - 3, y - 3, 6, 6);

        // Draw light border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(x - 3, y - 3, 6, 6);
    }

    private void drawStation(Station station, GraphicsContext gc) {
        // Draw station background
        gc.setFill(station.isLocked() ? Color.RED : Color.GREEN);
        double x = transformX(station.getX());
        double y = transformY(station.getY());
        gc.fillRect(x - 10, y - 10, 20, 20);

        // Draw station border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(x - 10, y - 10, 20, 20);

        // Draw station name
        gc.setFill(Color.WHITE);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(station.getName(), x, y + 4);
    }

    private void drawTrain(Train train, GraphicsContext gc) {
        Color trainColor;
        switch (train.getStatus()) {
            case IDLE: trainColor = Color.GRAY; break;
            case SEEKING_PATH: trainColor = Color.YELLOW; break;
            case LOCKING_PATH: trainColor = Color.PURPLE; break;
            case MOVING: trainColor = Color.GREEN; break;
            default: trainColor = Color.BLACK;
        }

        double x = transformX(train.getX());
        double y = transformY(train.getY());

        // Draw train background
        gc.setFill(trainColor);
        gc.fillOval(x - 7, y - 7, 14, 14);

        // Draw train border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(x - 7, y - 7, 14, 14);
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

        // Start train thread
        Thread trainThread = new Thread(train);
        trainThread.setDaemon(true);
        trainThread.start();

        // Set destination to start movement
        train.setDestination(dest);

        drawRailNetwork();
    }

    @Override
    public void updateTrain(Train train) {
        Platform.runLater(this::drawRailNetwork);
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
            System.out.println("Example: java SmartRailGUI simple.txt");
            System.exit(1);
        }
        launch(args);
    }
}