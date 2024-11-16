import java.util.List;
import java.util.ArrayList;
import javafx.application.Platform;

public class Train extends Component {
    public enum Status {
        IDLE,
        SEEKING_PATH,
        LOCKING_PATH,
        MOVING
    }

    private Status status = Status.IDLE;
    private Component currentLocation;
    private Component destination;
    private List<Component> currentPath;
    private int currentPathIndex;
    private TrainGUI gui;
    private static final long STATION_DELAY = 2000; // 2 second delay at stations

    public Train(double x, double y, TrainGUI gui) {
        super(x, y);
        this.gui = gui;
        this.status = Status.IDLE;
        System.out.println("Created new Train at (" + x + "," + y + ")");
    }

    public void setInitialLocation(Station station) {
        this.currentLocation = station;
        this.x = station.getX();
        this.y = station.getY();
        station.lock(this);
        updateGUI();
        System.out.println("Train set initial location: " + station.getName());
    }

    public void setDestination(Station destination) {
        if (status != Status.IDLE) {
            System.out.println("Train busy, cannot set destination");
            return;
        }
        this.destination = destination;
        this.status = Status.SEEKING_PATH;
        System.out.println("Train seeking path to: " + destination.getName());
        findPath();
        updateGUI();
    }

    private void findPath() {
        Message msg = new Message(Message.Type.FIND_PATH, this, currentLocation, destination);
        System.out.println("Sending FIND_PATH message from: " + currentLocation.getId());
        sendMessage(msg, currentLocation);
    }

    @Override
    protected void processMessage(Message msg) {
        System.out.println("Train received message: " + msg.getType());
        switch (msg.getType()) {
            case PATH_RESPONSE:
                handlePathResponse(msg);
                break;
            case LOCK_RESPONSE:
                handleLockResponse(msg);
                break;
            case MOVE_COMPLETE:
                handleMoveComplete(msg);
                break;
        }
        updateGUI();
    }

    private void handlePathResponse(Message msg) {
        System.out.println("Train handling path response");
        if (msg.getPath() != null && !msg.getPath().isEmpty()) {
            currentPath = new ArrayList<>(msg.getPath());
            currentPathIndex = 0;
            status = Status.LOCKING_PATH;
            System.out.println("Path found! Path size: " + currentPath.size());
            System.out.println("Path components: ");
            for (Component c : currentPath) {
                System.out.println("- " + c.getId());
            }
            requestLock();
        } else {
            status = Status.IDLE;
            System.out.println("No path found!");
            gui.showPathError(this);
        }
    }

    private void handleLockResponse(Message msg) {
        System.out.println("Train handling lock response, success: " + msg.isSuccess());
        if (msg.isSuccess()) {
            status = Status.MOVING;
            System.out.println("Starting movement along path");
            startMoving();
        } else {
            unlockPath();
            status = Status.IDLE;
            System.out.println("Failed to lock path!");
            gui.showLockError(this);
        }
    }

    private void handleMoveComplete(Message msg) {
        System.out.println("Train handling move complete");
        Component previousLocation = currentLocation;
        currentLocation = msg.getSource();
        this.x = currentLocation.getX();
        this.y = currentLocation.getY();

        if (previousLocation != currentLocation) {
            Message unlockMsg = new Message(Message.Type.UNLOCK_REQUEST, this, this, previousLocation);
            sendMessage(unlockMsg, previousLocation);
            System.out.println("Unlocked previous location: " + previousLocation.getId());
        }

        if (currentLocation == destination) {
            System.out.println("Reached destination!");
            status = Status.IDLE;
            currentPath = null;
            currentPathIndex = 0;
        } else {
            System.out.println("Moving to next component");
            // Add delay before moving to next segment
            new Thread(() -> {
                try {
                    Thread.sleep(STATION_DELAY);
                    moveToNext();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        updateGUI();
    }

    private void requestLock() {
        Message msg = new Message(Message.Type.LOCK_REQUEST, this, currentLocation, destination);
        msg.setPath(currentPath);
        System.out.println("Requesting lock for path");
        sendMessage(msg, currentPath.get(0));
    }

    private void startMoving() {
        System.out.println("Starting movement");
        moveToNext();
    }

    private void moveToNext() {
        if (currentPath != null && currentPathIndex < currentPath.size() - 1) {
            Component next = currentPath.get(currentPathIndex + 1);
            System.out.println("Moving to next component: " + next.getId());
            Message msg = new Message(Message.Type.MOVE_REQUEST, this, currentLocation, next);
            sendMessage(msg, next);
            currentPathIndex++;
        }
    }

    private void unlockPath() {
        if (currentPath != null) {
            System.out.println("Unlocking path");
            for (Component component : currentPath) {
                Message msg = new Message(Message.Type.UNLOCK_REQUEST, this, this, component);
                sendMessage(msg, component);
            }
            currentPath = null;
        }
    }

    public void updateGUI() {
        Platform.runLater(() -> gui.updateTrain(this));
    }

    public Status getStatus() {
        return status;
    }

    public Component getCurrentLocation() {
        return currentLocation;
    }

    public Component getDestination() {
        return destination;
    }
}