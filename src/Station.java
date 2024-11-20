import java.util.*;

/**
 * Represents a station in the rail system.
 * A station is an endpoint where trains can start and end their journeys.
 * Handles path finding, train movement, and station locking/unlocking.
 */
public class Station extends Component {
    /** Unique name identifier for this station */
    private final String name;

    /** Set to track processed message IDs and prevent duplicate processing */
    private final Set<String> processedMessageIds = new HashSet<>();

    /**
     * Creates a new station at specified coordinates.
     * @param name Station identifier
     * @param x X-coordinate in the rail system
     * @param y Y-coordinate in the rail system
     */
    public Station(String name, double x, double y) {
        super(x, y);
        this.name = name;
    }

    /**
     * Processes incoming messages for this station.
     * Handles path finding, movement requests, and locking/unlocking operations.
     * Implements duplicate message detection.
     * @param msg The message to process
     */
    @Override
    protected void processMessage(Message msg) {
        if (!processedMessageIds.add(msg.getMessageId())) {
            return;
        }

        switch (msg.getType()) {
            case FIND_PATH -> handleFindPath(msg);
            case PATH_RESPONSE -> handlePathResponse(msg);
            case LOCK_REQUEST -> handleLockRequest(msg);
            case MOVE_REQUEST -> handleMoveRequest(msg);
            case UNLOCK_REQUEST -> handleUnlockRequest(msg);
        }
    }

    /**
     * Handles path finding requests.
     * If this station is the destination, returns path to train.
     * Otherwise, forwards request to neighbors.
     * @param msg Path finding request message
     */
    private void handleFindPath(Message msg) {
        if (this == msg.getDestination()) {
            // This station is the destination, create path back to train
            List<Component> path = new ArrayList<>();
            path.add(this);
            Message response = new Message(Message.Type.PATH_RESPONSE,
                    msg.getTrain(), this, msg.getTrain());
            response.setPath(path);
            response.setSuccess(true);
            sendMessage(response, msg.getTrain());
        } else {
            // Forward find path request to neighbors except the source
            for (Component neighbor : getNeighbors()) {
                if (neighbor != msg.getSource()) {
                    Message newMsg = new Message(msg);
                    sendMessage(newMsg, neighbor);
                }
            }
        }
    }

    /**
     * Handles path response messages during path finding.
     * Adds this station to the path and forwards to train.
     * @param msg Path response message
     */
    private void handlePathResponse(Message msg) {
        if (msg.getPath() != null) {
            List<Component> newPath = new ArrayList<>(msg.getPath());
            newPath.add(0, this);
            msg.setPath(newPath);
        }
        sendMessage(msg, msg.getTrain());
    }

    /**
     * Handles requests to lock this station.
     * Locks station if available and forwards lock request along path.
     * @param msg Lock request message
     */
    private void handleLockRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());

            List<Component> path = msg.getPath();
            if (path != null && path.contains(this)) {
                int currentIndex = path.indexOf(this);
                if (currentIndex >= 0 && currentIndex < path.size() - 1) {
                    Component nextInPath = path.get(currentIndex + 1);
                    Message newMsg = new Message(msg);
                    sendMessage(newMsg, nextInPath);
                }
            }

            Message response = new Message(Message.Type.LOCK_RESPONSE,
                    msg.getTrain(), this, msg.getTrain());
            response.setSuccess(true);
            sendMessage(response, msg.getTrain());
        } else {
            Message response = new Message(Message.Type.LOCK_RESPONSE,
                    msg.getTrain(), this, msg.getTrain());
            response.setSuccess(false);
            sendMessage(response, msg.getTrain());
        }
    }

    /**
     * Handles train movement requests.
     * Locks station and notifies train of successful movement.
     * @param msg Move request message
     */
    private void handleMoveRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            Message complete = new Message(Message.Type.MOVE_COMPLETE,
                    msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(true);
            sendMessage(complete, msg.getTrain());
        }
    }

    /**
     * Handles requests to unlock this station.
     * Only unlocks if requested by occupying train.
     * @param msg Unlock request message
     */
    private void handleUnlockRequest(Message msg) {
        if (occupyingTrain == msg.getTrain()) {
            unlock();
        }
    }

    /**
     * Gets the name of this station.
     * @return Station name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns string representation of this station.
     * @return String containing station name and coordinates
     */
    @Override
    public String toString() {
        return name + " at (" + getX() + "," + getY() + ")";
    }
}