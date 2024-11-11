import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Track extends Component {
    private final double startX, startY, endX, endY;
    private final int segments;
    private Set<String> processedMessageIds = new HashSet<>();

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
            case FIND_PATH:
                handleFindPath(msg);
                break;
            case PATH_RESPONSE:
                handlePathResponse(msg);
                break;
            case LOCK_REQUEST:
                handleLockRequest(msg);
                break;
            case LOCK_RESPONSE:
                handleLockResponse(msg);
                break;
            case MOVE_REQUEST:
                handleMoveRequest(msg);
                break;
            case UNLOCK_REQUEST:
                handleUnlockRequest(msg);
                break;
        }
    }

    private void handleFindPath(Message msg) {
        for (Component neighbor : neighbors) {
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
        sendMessage(msg, msg.getDestination());
    }

    private void handleLockRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            for (Component neighbor : neighbors) {
                if (msg.getPath().contains(neighbor)) {
                    Message newMsg = new Message(msg);
                    sendMessage(newMsg, neighbor);
                }
            }
        } else {
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(false);
            sendMessage(response, msg.getTrain());
        }
    }

    private void handleLockResponse(Message msg) {
        sendMessage(msg, msg.getTrain());
    }

    private void handleMoveRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            sendMessage(complete, msg.getTrain());
        }
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
}