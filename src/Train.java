/**
 * Represents a train in the rail system.
 * Manages autonomous movement between stations, including path finding,
 * path locking, and movement coordination.
 */
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import javafx.application.Platform;

public class Train extends Component {
    /**
     * Possible states of a train in the system:
     * IDLE - Waiting at station for destination assignment
     * SEEKING_PATH - Attempting to find route to destination
     * LOCKING_PATH - Attempting to secure route components
     * MOVING - Actively moving along secured route
     * EXITED - Reached destination and removed from system
     */
    public enum Status {
        IDLE,
        SEEKING_PATH,
        LOCKING_PATH,
        MOVING,
        EXITED
    }
    // Current operational status of the train
    private Status status = Status.IDLE;
    // Current station location
    private Station currentStation;
    // Target destination station
    private Station destination;
    // Current path being followed
    private List<Component> currentPath;
    // Current position in path
    private int currentPathIndex;
    // Reference to GUI for updates
    private final TrainGUI gui;
    // Delay between path-finding retry attempts
    private static final long RETRY_DELAY = 1000;
    // Delay between movement steps
    private static final long MOVEMENT_DELAY = 500;

    /** Counter for lock retry attempts */
    private int lockRetryCount = 0;

    /** Maximum number of lock retry attempts before failing */
    private static final int MAX_LOCK_RETRIES = 3;

    /**
     * Creates a new train at specified coordinates.
     * @param x Initial X-coordinate
     * @param y Initial Y-coordinate
     * @param gui Reference to GUI for updates
     */
    public Train(double x, double y, TrainGUI gui) {
        super(x, y);
        this.gui = gui;
        this.status = Status.IDLE;
    }

    /**
     * Sets the initial location of the train.
     * Locks the starting station and updates position.
     * @param station Starting station
     */
    public void setInitialLocation(Station station) {
        this.currentStation = station;
        updatePosition(station);
        station.lock(this);
        updateGUI();
    }

    /**
     * Sets destination and initiates path finding.
     * Only works if train is in IDLE state.
     * @param destination Target station
     */
    public void setDestination(Station destination) {
        if (status != Status.IDLE) {
            System.out.println("Train " + getId() + " " +
                    "is busy, cannot set destination");
            return;
        }

        if (destination == currentStation) {
            System.out.println("Train " + getId() + " " +
                    "already at destination");
            return;
        }

        this.destination = destination;
        status = Status.SEEKING_PATH;
        findPath();
        updateGUI();
    }

    /**
     * Initiates path finding to destination.
     * Sends FIND_PATH message through rail network.
     */
    private void findPath() {
        System.out.println("Train " + getId() + " seeking path from " +
                currentStation.getId() + " to " + destination.getId());
        Message msg = new Message(Message.Type.FIND_PATH, this,
                currentStation, destination);
        sendMessage(msg, currentStation);
    }

    /**
     * Processes responses from rail network components.
     * Handles path finding results, lock responses, and movement completion.
     * @param msg Message to process
     */
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

    /**
     * Handles path finding response messages.
     * If path found, begins locking process, otherwise shows error.
     * @param msg Path response message
     */
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

    /**
     * Handles lock response messages.
     * Continues locking path or starts movement if complete.
     * @param msg Lock response message
     */
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
                System.out.println("Train " + getId() + " locked complete " +
                        "path, starting movement");
                startMoving();
            }
        } else {
            // Lock failed, retry or give up
            handleLockFailure();
        }
    }

    /**
     * Handles lock acquisition failures.
     * Implements retry mechanism with delay.
     * Gives up after MAX_LOCK_RETRIES attempts.
     */
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
            System.out.println("Train " + getId() + " failed to lock " +
                    "path after " +
                    MAX_LOCK_RETRIES + " attempts");
            unlockPath();
            status = Status.IDLE;
            gui.showLockError(this);
        }
    }

    /**
     * Handles movement completion messages.Updates position and handles station
     * arrivals.
     * @param msg Movement completion message
     */
    private void handleMoveComplete(Message msg) {
        if (status != Status.MOVING) return;

        if (msg.isSuccess()) {
            Component current = msg.getSource();

            // If we reached a station, update our current station
            if (current instanceof Station) {
                Station previousStation = currentStation;
                currentStation = (Station) current;

                // Unlock the previous station if it's different
                if (previousStation != null && previousStation !=
                        currentStation) {
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
            System.out.println("Train " + getId() + " movement failed, " +
                    "retrying path finding");
            status = Status.SEEKING_PATH;
            unlockPath();
            findPath();
        }
    }

    /**
     * Handles arrival at destination.
     * Cleans up and removes train from system.
     */
    private void reachDestination() {
        System.out.println("Train " + getId() + " reached destination " +
                destination.getId());
        unlockPath();
        status = Status.EXITED;
        updateGUI();
        gui.enableOtherTrains();
        gui.removeTrainFromSystem(this);
    }

    /**
     * Sends lock request for next component in path.
     */
    private void requestLock() {
        Component target = currentPath.get(currentPathIndex);
        Message msg = new Message(Message.Type.LOCK_REQUEST, this,
                currentStation, destination);
        msg.setPath(currentPath);
        System.out.println("Train " + getId() + " requesting lock for "
                + target.getId());
        sendMessage(msg, target);
    }

    /**
     * Begins movement along locked path.
     */
    private void startMoving() {
        moveToNext();
    }

    /**
     * Moves to next component in path
     */
    private void moveToNext() {
        if (currentPath == null || currentPathIndex >= currentPath.size()) {
            System.out.println("Train " + getId() + " has no more components " +
                    "to move to");
            return;
        }

        Component next = currentPath.get(currentPathIndex);
        System.out.println("Train " + getId() + " moving to " + next.getId() +
                " (" + (currentPathIndex + 1) + "/" + currentPath.size() + ")");

        Message msg = new Message(Message.Type.MOVE_REQUEST, this,
                currentStation, next);
        sendMessage(msg, next);
    }

    /**
     * Unlocks all components in current path.
     */
    private void unlockPath() {
        if (currentPath != null) {
            for (Component component : currentPath) {
                unlockComponent(component);
            }
        }
    }

    /**
     * Sends unlock request to component.
     * @param component component to unlock
     */
    private void unlockComponent(Component component) {
        Message msg = new Message(Message.Type.UNLOCK_REQUEST, this,
                this, component);
        sendMessage(msg, component);
    }

    /**
     * Updates train position to match component.
     * @param component component to update
     */
    private void updatePosition(Component component) {
        this.x = component.getX();
        this.y = component.getY();
    }

    /**
     * Updates GUI display
     */
    public void updateGUI() {
        Platform.runLater(() -> gui.updateTrain(this));
    }

    /**
     * Gets current operational status of train.
     * @return Current Status value
     */
    public Status getStatus() {
        return status;
    }
    /**
     * Returns string representation of train including position and status.
     * @return string describing train statee
     */
    @Override
    public String toString() {
        return getId() + " at (" + x + "," + y + ") status: " + status;
    }
}