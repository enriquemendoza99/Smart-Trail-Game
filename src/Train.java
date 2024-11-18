import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
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

    public Train(double x, double y, TrainGUI gui) {
        super(x, y);
        this.gui = gui;
        this.status = Status.IDLE;
        System.out.println("Created new Train at (" + x + "," + y + ")");
    }

    public void setInitialLocation(Station station) {
        this.currentLocation = station;
        updatePosition(station);
        station.lock(this);
        updateGUI();
        System.out.println("Train set initial location: " + station.getName());
    }

    private void updatePosition(Component component) {
        this.x = component.getX();
        this.y = component.getY();
        System.out.println("Updated train position to: (" + x + "," + y + ")");
    }

    public void setDestination(Station destination) {
        if (status != Status.IDLE) {
            System.out.println("Train busy, cannot set destination");
            return;
        }

        if (destination == currentLocation) {
            System.out.println("Train already at destination");
            return;
        }

        this.destination = destination;
        status = Status.SEEKING_PATH;
        System.out.println("Train seeking path to: " + destination.getName() + ", current status: " + status);
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
        System.out.println("Train received message: " + msg.getType() + ", current status: " + status);
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
    }

    private void handlePathResponse(Message msg) {
        System.out.println("Train handling path response");
        if (msg.getPath() != null && !msg.getPath().isEmpty()) {
            currentPath = new ArrayList<>(msg.getPath());
            Collections.reverse(currentPath);
            currentPathIndex = 0;
            status = Status.LOCKING_PATH;
            System.out.println("Found path with " + currentPath.size() + " components:");
            for (Component c : currentPath) {
                System.out.println("  - " + c.getId());
            }
            requestLock();
        } else {
            status = Status.IDLE;
            System.out.println("No path found!");
            gui.showPathError(this);
        }
        updateGUI();
    }

    private void handleLockResponse(Message msg) {
        System.out.println("Train handling lock response, success: " + msg.isSuccess() + ", current status: " + status);
        if (msg.isSuccess() && status == Status.LOCKING_PATH) {
            status = Status.MOVING;
            System.out.println("Path locked, starting movement. New status: " + status);
            startMoving();
        } else {
            unlockPath();
            status = Status.IDLE;
            System.out.println("Failed to lock path! New status: " + status);
            gui.showLockError(this);
        }
        updateGUI();
    }

    private void handleMoveComplete(Message msg) {
        System.out.println("Train handling move complete");
        if (status != Status.MOVING) {
            System.out.println("Ignoring move complete because train is not in MOVING state");
            return;
        }

        Component previousLocation = currentLocation;
        currentLocation = msg.getSource();

        if (previousLocation != null && previousLocation != currentLocation) {
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
            System.out.println("Moving to next component in path");
            moveToNext();
        }
        updateGUI();
    }

    private void requestLock() {
        Message msg = new Message(Message.Type.LOCK_REQUEST, this, currentLocation, destination);
        msg.setPath(currentPath);
        System.out.println("Requesting lock for path");
        sendMessage(msg, currentPath.get(currentPathIndex));
    }

    private void startMoving() {
        System.out.println("Starting movement");
        moveToNext();
    }

    private void moveToNext() {
        if (currentPath != null && currentPathIndex < currentPath.size() - 1) {
            Component next = currentPath.get(currentPathIndex + 1);
            System.out.println("Moving to next component: " + next.getId());

            if (next instanceof Track) {
                Track track = (Track) next;
                // Set initial position at start of track
                this.x = track.getStartX();
                this.y = track.getStartY();
                updateGUI();
            }

            Message msg = new Message(Message.Type.MOVE_REQUEST, this, currentLocation, next);
            sendMessage(msg, next);
        }
    }

    private void unlockPath() {
        if (currentPath != null) {
            System.out.println("Unlocking path components:");
            for (Component component : currentPath) {
                Message msg = new Message(Message.Type.UNLOCK_REQUEST, this, this, component);
                sendMessage(msg, component);
            }
            currentPath = null;
        }
    }

    public void updateGUI() {
        Platform.runLater(() -> {
            gui.updateTrain(this);
            System.out.println("Updated GUI with train position: (" + x + "," + y + "), status: " + status);
        });
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
