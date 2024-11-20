import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Track extends Component {
    private final double startX, startY, endX, endY;
    private final int segments;
    private final Set<String> processedMessageIds = new HashSet<>();
    private static final int MOVEMENT_STEPS = 50;
    private static final long MOVEMENT_DELAY = 50; // Faster movement

    public Track(double startX, double startY, double endX, double endY, int segments) {
        super(startX, startY);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.segments = segments;
    }

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

    private void handleFindPath(Message msg) {
        for (Component neighbor : getNeighbors()) {
            if (neighbor != msg.getSource()) {
                Message newMsg = new Message(msg);
                sendMessage(newMsg, neighbor);
            }
        }
    }

    private void handlePathResponse(Message msg) {
        if (msg.getPath() != null) {
            List<Component> newPath = new ArrayList<>(msg.getPath());
            newPath.add(0, this);
            msg.setPath(newPath);
        }
        sendMessage(msg, msg.getTrain());
    }

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

            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(true);
            sendMessage(response, msg.getTrain());
        } else {
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(false);
            sendMessage(response, msg.getTrain());
        }
    }

    private void handleMoveRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());

            Thread moveThread = new Thread(() -> {
                try {
                    moveTrainAlongTrack(msg.getTrain());
                    Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
                    complete.setSuccess(true);
                    sendMessage(complete, msg.getTrain());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            moveThread.setDaemon(true);
            moveThread.start();
        } else {
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(false);
            sendMessage(complete, msg.getTrain());
        }
    }

    private void moveTrainAlongTrack(Train train) throws InterruptedException {
        for (int i = 0; i <= MOVEMENT_STEPS; i++) {
            double progress = (double) i / MOVEMENT_STEPS;
            train.x = startX + (endX - startX) * progress;
            train.y = startY + (endY - startY) * progress;
            train.updateGUI();
            Thread.sleep(MOVEMENT_DELAY / segments);
        }

        // Ensure train reaches exact endpoint
        train.x = endX;
        train.y = endY;
        train.updateGUI();
    }

    private void handleUnlockRequest(Message msg) {
        if (occupyingTrain == msg.getTrain()) {
            unlock();
        }
    }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
    public int getSegments() { return segments; }

    @Override
    public String toString() {
        return String.format("%s [(%f,%f) to (%f,%f)]", getId(), startX, startY, endX, endY);
    }
}