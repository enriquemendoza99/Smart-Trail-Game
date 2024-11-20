import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javafx.application.Platform;

public class Train extends Component {
    public enum Status {
        IDLE,
        SEEKING_PATH,
        LOCKING_PATH,
        MOVING,
        EXITED
    }

    private Status status = Status.IDLE;
    private Station currentStation;
    private Station destination;
    private List<Component> currentPath;
    private int currentPathIndex;
    private final TrainGUI gui;
    private static final long RETRY_DELAY = 1000; // 1 second delay before retrying path
    private static final long MOVEMENT_DELAY = 500; // 0.5 second between movements
    private int lockRetryCount = 0;
    private static final int MAX_LOCK_RETRIES = 3;

    public Train(double x, double y, TrainGUI gui) {
        super(x, y);
        this.gui = gui;
        this.status = Status.IDLE;
    }

    public void setInitialLocation(Station station) {
        this.currentStation = station;
        updatePosition(station);
        station.lock(this);
        updateGUI();
    }

    public void setDestination(Station destination) {
        if (status != Status.IDLE) {
            System.out.println("Train " + getId() + " is busy, cannot set destination");
            return;
        }

        if (destination == currentStation) {
            System.out.println("Train " + getId() + " already at destination");
            return;
        }

        this.destination = destination;
        status = Status.SEEKING_PATH;
        findPath();
        updateGUI();
    }

    private void findPath() {
        System.out.println("Train " + getId() + " seeking path from " +
                currentStation.getId() + " to " + destination.getId());
        Message msg = new Message(Message.Type.FIND_PATH, this, currentStation, destination);
        sendMessage(msg, currentStation);
    }

    @Override
    protected void processMessage(Message msg) {
        System.out.println("Train " + getId() + " received " + msg.getType() +
                " message, current status: " + status);

        switch (msg.getType()) {
            case PATH_RESPONSE -> handlePathResponse(msg);
            case LOCK_RESPONSE -> handleLockResponse(msg);
            case MOVE_COMPLETE -> handleMoveComplete(msg);
        }
        updateGUI();
    }

    private void handlePathResponse(Message msg) {
        if (msg.getPath() != null && !msg.getPath().isEmpty()) {
            currentPath = new ArrayList<>(msg.getPath());
            currentPathIndex = 0;
            lockRetryCount = 0;
            status = Status.LOCKING_PATH;
            System.out.println("Train " + getId() + " found path with " +
                    currentPath.size() + " components");
            requestLock();
        } else {
            System.out.println("Train " + getId() + " failed to find path");
            status = Status.IDLE;
            gui.showPathError(this);
        }
    }

    private void handleLockResponse(Message msg) {
        if (msg.isSuccess() && status == Status.LOCKING_PATH) {
            if (currentPathIndex < currentPath.size() - 1) {
                // Continue locking next component in path
                currentPathIndex++;
                requestLock();
            } else {
                // All components locked, start moving
                status = Status.MOVING;
                currentPathIndex = 0;
                System.out.println("Train " + getId() + " locked complete path, starting movement");
                startMoving();
            }
        } else {
            // Lock failed, retry or give up
            handleLockFailure();
        }
    }

    private void handleLockFailure() {
        lockRetryCount++;
        if (lockRetryCount <= MAX_LOCK_RETRIES) {
            System.out.println("Train " + getId() + " lock failed, retrying (" +
                    lockRetryCount + "/" + MAX_LOCK_RETRIES + ")");
            unlockPath();
            try {
                Thread.sleep(RETRY_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            currentPathIndex = 0;
            requestLock();
        } else {
            System.out.println("Train " + getId() + " failed to lock path after " +
                    MAX_LOCK_RETRIES + " attempts");
            unlockPath();
            status = Status.IDLE;
            gui.showLockError(this);
        }
    }

    private void handleMoveComplete(Message msg) {
        if (status != Status.MOVING) return;

        if (msg.isSuccess()) {
            Component current = msg.getSource();

            // If we reached a station, update our current station
            if (current instanceof Station) {
                Station previousStation = currentStation;
                currentStation = (Station) current;

                // Unlock the previous station if it's different
                if (previousStation != null && previousStation != currentStation) {
                    unlockComponent(previousStation);
                }

                // Check if we reached the destination
                if (current == destination) {
                    reachDestination();
                    return;
                }
            }

            // Move to next component if not at destination
            currentPathIndex++;
            if (currentPathIndex < currentPath.size()) {
                moveToNext();
            }
        } else {
            // Movement failed, retry path finding
            System.out.println("Train " + getId() + " movement failed, retrying path finding");
            status = Status.SEEKING_PATH;
            unlockPath();
            findPath();
        }
    }

    private void reachDestination() {
        System.out.println("Train " + getId() + " reached destination " + destination.getId());
        unlockPath();
        status = Status.EXITED;
        updateGUI();
        gui.enableOtherTrains();
        gui.removeTrainFromSystem(this);
    }

    private void requestLock() {
        Component target = currentPath.get(currentPathIndex);
        Message msg = new Message(Message.Type.LOCK_REQUEST, this, currentStation, destination);
        msg.setPath(currentPath);
        System.out.println("Train " + getId() + " requesting lock for " + target.getId());
        sendMessage(msg, target);
    }

    private void startMoving() {
        moveToNext();
    }

    private void moveToNext() {
        if (currentPath == null || currentPathIndex >= currentPath.size()) {
            System.out.println("Train " + getId() + " has no more components to move to");
            return;
        }

        Component next = currentPath.get(currentPathIndex);
        System.out.println("Train " + getId() + " moving to " + next.getId() +
                " (" + (currentPathIndex + 1) + "/" + currentPath.size() + ")");

        Message msg = new Message(Message.Type.MOVE_REQUEST, this, currentStation, next);
        sendMessage(msg, next);
    }

    private void unlockPath() {
        if (currentPath != null) {
            for (Component component : currentPath) {
                unlockComponent(component);
            }
        }
    }

    private void unlockComponent(Component component) {
        Message msg = new Message(Message.Type.UNLOCK_REQUEST, this, this, component);
        sendMessage(msg, component);
    }

    private void updatePosition(Component component) {
        this.x = component.getX();
        this.y = component.getY();
    }

    public void updateGUI() {
        Platform.runLater(() -> gui.updateTrain(this));
    }

    public Status getStatus() {
        return status;
    }

    public Station getCurrentStation() {
        return currentStation;
    }

    public Station getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return getId() + " at (" + x + "," + y + ") status: " + status;
    }
}