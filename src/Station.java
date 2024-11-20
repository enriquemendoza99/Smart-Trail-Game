import java.util.*;

public class Station extends Component {
    private final String name;
    private final Set<String> processedMessageIds = new HashSet<>();

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
            case FIND_PATH -> handleFindPath(msg);
            case PATH_RESPONSE -> handlePathResponse(msg);
            case LOCK_REQUEST -> handleLockRequest(msg);
            case MOVE_REQUEST -> handleMoveRequest(msg);
            case UNLOCK_REQUEST -> handleUnlockRequest(msg);
        }
    }

    private void handleFindPath(Message msg) {
        if (this == msg.getDestination()) {
            // This station is the destination, create path back to train
            List<Component> path = new ArrayList<>();
            path.add(this);
            Message response = new Message(Message.Type.PATH_RESPONSE, msg.getTrain(), this, msg.getTrain());
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
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(true);
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
        return name + " at (" + getX() + "," + getY() + ")";
    }
}