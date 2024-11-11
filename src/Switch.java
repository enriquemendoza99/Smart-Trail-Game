import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Switch extends Component {
    private Component mainTrack;
    private Component altTrack;
    private boolean isMainPosition = true;
    private Set<String> processedMessageIds = new HashSet<>();

    public Switch(double x, double y) {
        super(x, y);
    }

    public void setTracks(Component mainTrack, Component altTrack) {
        this.mainTrack = mainTrack;
        this.altTrack = altTrack;
        addNeighbor(mainTrack);
        addNeighbor(altTrack);
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
            sendMessage(msg, msg.getDestination());
        }
    }

    private void handleLockRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            List<Component> path = msg.getPath();
            int currentIndex = path.indexOf(this);
            if (currentIndex >= 0 && currentIndex < path.size() - 1) {
                Component nextComponent = path.get(currentIndex + 1);
                isMainPosition = nextComponent == mainTrack;
            }

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