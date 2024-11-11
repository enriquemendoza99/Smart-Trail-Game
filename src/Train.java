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

    public Train(double x, double y, TrainGUI gui) {
        super(x, y);
        this.gui = gui;
        this.status = Status.IDLE;
    }

    public void setInitialLocation(Station station) {
        this.currentLocation = station;
        this.x = station.getX();
        this.y = station.getY();
        station.lock(this);
    }

    public void setDestination(Station destination) {
        if (status != Status.IDLE) {
            return;
        }
        this.destination = destination;
        this.status = Status.SEEKING_PATH;
        findPath();
        updateGUI();
    }

    private void findPath() {
        Message msg = new Message(Message.Type.FIND_PATH, this, currentLocation, destination);
        sendMessage(msg, currentLocation);
    }

    @Override
    protected void processMessage(Message msg) {
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
        if (msg.getPath() != null && !msg.getPath().isEmpty()) {
            currentPath = new ArrayList<>(msg.getPath());
            currentPathIndex = 0;
            status = Status.LOCKING_PATH;
            requestLock();
        } else {
            status = Status.IDLE;
            gui.showPathError(this);
        }
    }

    private void handleLockResponse(Message msg) {
        if (msg.isSuccess()) {
            status = Status.MOVING;
            startMoving();
        } else {
            unlockPath();
            status = Status.IDLE;
            gui.showLockError(this);
        }
    }

    private void handleMoveComplete(Message msg) {
        if (currentLocation == destination) {
            status = Status.IDLE;
            unlockPath();
        } else {
            moveToNext();
        }
    }

    private void requestLock() {
        Message msg = new Message(Message.Type.LOCK_REQUEST, this, currentLocation, destination);
        msg.setPath(currentPath);
        sendMessage(msg, currentPath.get(0));
    }

    private void startMoving() {
        moveToNext();
    }

    private void moveToNext() {
        if (currentPathIndex < currentPath.size() - 1) {
            Component next = currentPath.get(currentPathIndex + 1);
            Message msg = new Message(Message.Type.MOVE_REQUEST, this, currentLocation, next);
            sendMessage(msg, next);
            currentPathIndex++;
            this.x = next.getX();
            this.y = next.getY();
            currentLocation = next;
            updateGUI();
        }
    }

    private void unlockPath() {
        if (currentPath != null) {
            for (Component component : currentPath) {
                Message msg = new Message(Message.Type.UNLOCK_REQUEST, this, this, component);
                sendMessage(msg, component);
            }
            currentPath = null;
        }
    }

    private void updateGUI() {
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