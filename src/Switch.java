/**
 * Represents a switch/turnout in the rail system.
 * Controls connections between multiple tracks and manages switch position.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Switch extends Component {
    // Primary track connected to this switch
    private Component mainTrack;
    //Secondary track connected to this switch
    private Component altTrack;
    //Current switch position (true = main track, false = alternate track)
    private boolean isMainPosition = true;
    // Set to track processed message IDs and prevent duplicate processing
    private final Set<String> processedMessageIds = new HashSet<>();
    /**
     * Creates a new switch at specified coordinates.
     * Initially has no track connections.
     * @param x X-coordinate in the rail system
     * @param y Y-coordinate in the rail system
     */
    public Switch(double x, double y) {
        super(x, y);
    }
    /**
     * Configures the switch with its connected tracks and clears existing
     * connections and establishes new ones.
     * @param mainTrack Primary track connection
     * @param altTrack Secondary track connection
     */
    public void setTracks(Component mainTrack, Component altTrack) {
        this.mainTrack = mainTrack;
        this.altTrack = altTrack;

        neighbors.clear();
        if (mainTrack != null) {
            addNeighbor(mainTrack);
        }
        if (altTrack != null) {
            addNeighbor(altTrack);
        }

        System.out.println("Switch " + getId() + " configured with main=" +
                (mainTrack != null ? mainTrack.getId() : "none") +
                ", alt=" + (altTrack != null ? altTrack.getId() : "none"));
    }
    /**
     * Processes incoming messages for this switch.
     * Handles path finding, movement requests, and locking operations.
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
     * Handles path finding requests by forwarding to neighbors.
     * Excludes the source component to prevent cycles in path finding.
     * @param msg Path finding request message
     */
    private void handleFindPath(Message msg) {

        for (Component neighbor : getNeighbors()) {
            if (neighbor != msg.getSource()) {
                Message newMsg = new Message(msg);
                sendMessage(newMsg, neighbor);
            }
        }
    }
    /**
     * Handles path response messages during path finding.
     * @param msg Path response message
     */
    private void handlePathResponse(Message msg) {
        if (msg.getPath() != null) {
            List<Component> newPath = new ArrayList<>(msg.getPath());
            newPath.add(0, this);
            msg.setPath(newPath);

            // Check if we need to switch position based on path
            if (newPath.size() >= 2) {
                Component nextInPath = newPath.get(1);
                isMainPosition = nextInPath == mainTrack;
            }
        }
        sendMessage(msg, msg.getTrain());
    }
    /**
     * Handles requests to lock this switch.
     * Locks switch if available and forwards lock request along path.
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
                    isMainPosition = nextInPath == mainTrack;

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
     * Handles train movement requests through this switch.
     * Locks switch and notifies train of successful movement.
     * @param msg Move request message
     */
    private void handleMoveRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(true);
            sendMessage(complete, msg.getTrain());
        } else {
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(false);
            sendMessage(complete, msg.getTrain());
        }
    }

    /**
     * Handles requests to unlock this switch.
     * Only unlocks if requested by occupying train.
     * @param msg Unlock request message
     */
    private void handleUnlockRequest(Message msg) {
        if (occupyingTrain == msg.getTrain()) {
            unlock();
        }
    }

    /**
     * Gets the current position of the switch.
     * @return true if switch is in main position, false if in alternate position
     */
    public boolean isMainPosition() {
        return isMainPosition;
    }

    /**
     * Gets the main track connected to this switch.
     * @return The main track component
     */
    public Component getMainTrack() {
        return mainTrack;
    }

    /**
     * Gets the alternate track connected to this switch.
     * @return The alternate track component
     */
    public Component getAltTrack() {
        return altTrack;
    }
}