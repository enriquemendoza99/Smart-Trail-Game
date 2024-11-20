import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Switch extends Component {
    private Component mainTrack;
    private Component altTrack;
    private boolean isMainPosition = true;
    private final Set<String> processedMessageIds = new HashSet<>();

    public Switch(double x, double y) {
        super(x, y);
    }

    public void setTracks(Component mainTrack, Component altTrack) {
        this.mainTrack = mainTrack;
        this.altTrack = altTrack;

        // Ensure proper neighbor connections
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
        // Forward to all neighbors except the source
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

            // Check if we need to switch position based on path
            if (newPath.size() >= 2) {
                Component nextInPath = newPath.get(1);
                isMainPosition = nextInPath == mainTrack;
            }
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
                    isMainPosition = nextInPath == mainTrack;

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
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(true);
            sendMessage(complete, msg.getTrain());
        } else {
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(false);
            sendMessage(complete, msg.getTrain());
        }
    }

    private void handleUnlockRequest(Message msg) {
        if (occupyingTrain == msg.getTrain()) {
            unlock();
        }
    }

    public boolean isMainPosition() {
        return isMainPosition;
    }

    public Component getMainTrack() {
        return mainTrack;
    }

    public Component getAltTrack() {
        return altTrack;
    }
}