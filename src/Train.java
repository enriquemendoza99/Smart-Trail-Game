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
    private TrainGUI gui;
    private static final long MOVEMENT_DELAY = 1000; // 1 second delay between steps

    public Train(double x, double y, TrainGUI gui) {
        super(x, y);
        this.gui = gui;
        this.status = Status.IDLE;
        System.out.println("Created new Train at (" + x + "," + y + ")");
    }

    public void setInitialLocation(Station station) {
        this.currentStation = station;
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

        if (destination == currentStation) {
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
        Message msg = new Message(Message.Type.FIND_PATH, this, currentStation, destination);
        System.out.println("Sending FIND_PATH message from: " + currentStation.getId());
        sendMessage(msg, currentStation);
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
        updateGUI();
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
        System.out.println("Train handling move complete, current status: " + status);
        if (status != Status.MOVING) {
            System.out.println("Ignoring move complete because train is not in MOVING state");
            return;
        }

        Station previousStation = currentStation;
        currentStation = (Station) msg.getSource();

        if (previousStation != null && previousStation != currentStation) {
            Message unlockMsg = new Message(Message.Type.UNLOCK_REQUEST, this, this, previousStation);
            sendMessage(unlockMsg, previousStation);
            System.out.println("Unlocked previous station: " + previousStation.getId());
        }

        if (currentStation == destination) {
            System.out.println("Reached destination!");
            Message unlockMsg = new Message(Message.Type.UNLOCK_REQUEST, this, this, destination);
            sendMessage(unlockMsg, destination);
            System.out.println("Unlocked destination station: " + destination.getId());

            // Unlock the path components
            for (Component component : currentPath) {
                if (component instanceof Track) {
                    Message trackUnlockMsg = new Message(Message.Type.UNLOCK_REQUEST, this, this, component);
                    sendMessage(trackUnlockMsg, component);
                    System.out.println("Unlocked track: " + component.getId());
                }
            }

            status = Status.EXITED;
            currentPath = null;
            currentPathIndex = 0;
            updateGUI();
            gui.enableOtherTrains();
        } else {
            System.out.println("Moving to next component in path");
            moveToNext();
        }
    }

    private void requestLock() {
        Message msg = new Message(Message.Type.LOCK_REQUEST, this, currentStation, destination);
        msg.setPath(currentPath);
        System.out.println("Requesting lock for path starting with: " + currentPath.get(currentPathIndex).getId());
        sendMessage(msg, currentPath.get(currentPathIndex));
    }

    private void startMoving() {
        System.out.println("Starting train movement");
        moveToNext();
    }

    private void moveToNext() {
        if (currentPath != null && currentPathIndex < currentPath.size() - 1) {
            Component next = currentPath.get(currentPathIndex + 1);
            System.out.println("Moving to next component: " + next.getId() +
                    " (index " + (currentPathIndex + 1) + " of " + (currentPath.size() - 1) + ")");

            if (next instanceof Track) {
                Track track = (Track) next;
                // Start at the beginning of the track
                this.x = track.getStartX();
                this.y = track.getStartY();
                updateGUI();
                try {
                    Thread.sleep(MOVEMENT_DELAY); // 1 second delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else if (next instanceof Station) {
                this.currentStation = (Station) next;
                updatePosition(currentStation);
            }

            Message msg = new Message(Message.Type.MOVE_REQUEST, this, currentStation, next);
            sendMessage(msg, next);
        } else {
            System.out.println("No more components to move to");
            if (currentStation != destination) {
                updatePosition(destination);
                status = Status.EXITED;
                updateGUI();
            }
        }
    }

    private void unlockPath() {
        if (currentPath != null) {
            System.out.println("Unlocking path components:");
            for (Component component : currentPath) {
                if (component instanceof Station) {
                    System.out.println("  - Unlocking station " + component.getId());
                    Message msg = new Message(Message.Type.UNLOCK_REQUEST, this, this, component);
                    sendMessage(msg, component);
                } else if (component instanceof Track) {
                    System.out.println("  - Unlocking track " + component.getId());
                    Message msg = new Message(Message.Type.UNLOCK_REQUEST, this, this, component);
                    sendMessage(msg, component);
                }
            }
            currentPath = null;
        }
    }

    public void updateGUI() {
        Platform.runLater(() -> {
            if (status != Train.Status.EXITED) {
                gui.updateTrain(this);
                System.out.println("Updated GUI with train position: (" + x + "," + y + "), status: " + status);
            } else {
                System.out.println("Train has exited the system, removing it from GUI");
                gui.removeTrain(this);
            }
        });
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
}