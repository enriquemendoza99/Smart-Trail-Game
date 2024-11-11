import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Station extends Component {
    private String name;
    private Set<String> processedMessageIds = new HashSet<>();

    public Station(String name, double x, double y) {
        super(x, y);
        this.name = name;
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
            case LOCK_REQUEST:
                handleLockRequest(msg);
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
        if (this == msg.getDestination()) {
            List<Component> path = new ArrayList<>();
            path.add(this);
            Message response = new Message(Message.Type.PATH_RESPONSE, msg.getTrain(), this, msg.getSource());
            response.setPath(path);
            sendMessage(response, msg.getSource());
        } else {
            for (Component neighbor : neighbors) {
                if (neighbor != msg.getSource()) {
                    Message newMsg = new Message(msg);
                    sendMessage(newMsg, neighbor);
                }
            }
        }
    }

    private void handleLockRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getSource());
            response.setSuccess(true);
            sendMessage(response, msg.getSource());
        } else {
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getSource());
            response.setSuccess(false);
            sendMessage(response, msg.getSource());
        }
    }

    private void handleMoveRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getSource());
            sendMessage(complete, msg.getTrain());
        }
    }

    private void handleUnlockRequest(Message msg) {
        if (occupyingTrain == msg.getTrain()) {
            unlock();
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}