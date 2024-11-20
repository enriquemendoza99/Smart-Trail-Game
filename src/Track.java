/**
 * Represents a section of track in the rail system.
 * Handles train movement, path finding, and track segment management.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Track extends Component {
    // X-coordinate of track start point
    private final double startX;
    // Y-coordinate of track start point
    private final double startY;
    // X-coordinate of track end point
    private final double endX;
    //Y-coordinate of track end point
    private final double endY;
    // Number of segments this track is divided into
    private final int segments;
    // Set to track processed message IDs and prevent duplicate processing
    private final Set<String> processedMessageIds = new HashSet<>();
    //Number of steps for smooth train movement animation
    private static final int MOVEMENT_STEPS = 50;
    // Base delay between movement steps
    private static final long MOVEMENT_DELAY = 50;

    /**
     * Creates a new track section with specified endpoints and segments.
     * @param startX X-coordinate of start point
     * @param startY Y-coordinate of start point
     * @param endX X-coordinate of end point
     * @param endY Y-coordinate of end point
     * @param segments Number of segments to divide track into
     */
    public Track(double startX, double startY, double endX, double endY,
                 int segments) {
        super(startX, startY);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.segments = segments;
    }

    /**
     * Processes incoming messages for this track.
     * Implements duplicate message detection.
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
     * Adds this track to the path.
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
     * Handles requests to lock this track section.
     * Locks track if available and forwards lock request along path.
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
     * Handles train movement requests through this track section.
     * Creates a new thread to handle smooth movement animation.
     * @param msg Move request message
     */
    private void handleMoveRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());

            Thread moveThread = new Thread(() -> {
                try {
                    moveTrainAlongTrack(msg.getTrain());
                    Message complete = new Message(
                            Message.Type.MOVE_COMPLETE, msg.getTrain(),
                            this, msg.getTrain());
                    complete.setSuccess(true);
                    sendMessage(complete, msg.getTrain());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            moveThread.setDaemon(true);
            moveThread.start();
        } else {
            Message complete = new Message(Message.Type.MOVE_COMPLETE,
                    msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(false);
            sendMessage(complete, msg.getTrain());
        }
    }
    /**
     * Moves a train along this track section using smooth animation.
     * Adjusts movement speed based on number of segments.
     * Updates train position and GUI at each step.
     * @param train Train to move
     * @throws InterruptedException if movement is interrupted
     */
    private void moveTrainAlongTrack(Train train) throws InterruptedException {
        for (int i = 0; i <= MOVEMENT_STEPS; i++) {
            double progress = (double) i / MOVEMENT_STEPS;
            train.x = startX + (endX - startX) * progress;
            train.y = startY + (endY - startY) * progress;
            train.updateGUI();
            Thread.sleep(MOVEMENT_DELAY / segments);
        }

        train.x = endX;
        train.y = endY;
        train.updateGUI();
    }

    /**
     * Handles requests to unlock this track section.
     * Only unlocks if requested by occupying train.
     * @param msg Unlock request message
     */
    private void handleUnlockRequest(Message msg) {
        if (occupyingTrain == msg.getTrain()) {
            unlock();
        }
    }

    /**
     * Gets the X-coordinate of track start point.
     * @return Start X-coordinate
     */
    public double getStartX() { return startX; }

    /**
     * Gets the Y-coordinate of track start point.
     * @return Start Y-coordinate
     */
    public double getStartY() { return startY; }

    /**
     * Gets the X-coordinate of track end point.
     * @return End X-coordinate
     */
    public double getEndX() { return endX; }

    /**
     * Gets the Y-coordinate of track end point.
     * @return End Y-coordinate
     */
    public double getEndY() { return endY; }

    /**
     * Gets the number of segments in this track section.
     * @return Number of segments
     */
    public int getSegments() { return segments; }

    /**
     * Returns string representation of this track section.
     * @return String containing track ID and endpoint coordinates
     */
    @Override
    public String toString() {
        return String.format("%s [(%f,%f) to (%f,%f)]", getId(), startX,
                startY, endX, endY);
    }
}