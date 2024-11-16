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
        System.out.println("Created Station: " + name + " at (" + x + "," + y + ")");
    }

    @Override
    protected void processMessage(Message msg) {
        if (!processedMessageIds.add(msg.getMessageId())) {
            return;
        }

        System.out.println("Station " + name + " processing message: " + msg.getType());
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
            case MOVE_REQUEST:
                handleMoveRequest(msg);
                break;
            case UNLOCK_REQUEST:
                handleUnlockRequest(msg);
                break;
        }
    }

    private void handleFindPath(Message msg) {
        System.out.println("Station " + name + " handling find path request");
        if (this == msg.getDestination()) {
            System.out.println("Station " + name + " is destination, creating path");
            List<Component> path = new ArrayList<>();
            path.add(this);
            Message response = new Message(Message.Type.PATH_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setPath(path);
            response.setSuccess(true);
            System.out.println("Sending PATH_RESPONSE to train");
            sendMessage(response, msg.getTrain());
        } else {
            for (Component neighbor : neighbors) {
                if (neighbor != msg.getSource()) {
                    Message newMsg = new Message(msg);
                    System.out.println("Forwarding FIND_PATH to: " + neighbor.getId());
                    sendMessage(newMsg, neighbor);
                }
            }
        }
    }

    private void handlePathResponse(Message msg) {
        System.out.println("Station " + name + " handling path response");
        if (msg.getPath() != null) {
            List<Component> newPath = new ArrayList<>(msg.getPath());
            newPath.add(0, this);
            msg.setPath(newPath);
            System.out.println("Updated path size: " + newPath.size());
        }
        sendMessage(msg, msg.getTrain());
    }

    private void handleLockRequest(Message msg) {
        System.out.println("Station " + name + " handling lock request");
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(true);
            System.out.println("Station " + name + " sending successful lock response");
            sendMessage(response, msg.getTrain());
        } else {
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(false);
            System.out.println("Station " + name + " sending failed lock response");
            sendMessage(response, msg.getTrain());
        }
    }

    private void handleMoveRequest(Message msg) {
        System.out.println("Station " + name + " handling move request");
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(true);
            System.out.println("Station " + name + " sending move complete");
            sendMessage(complete, msg.getTrain());
        }
    }

    private void handleUnlockRequest(Message msg) {
        System.out.println("Station " + name + " handling unlock request");
        if (occupyingTrain == msg.getTrain()) {
            unlock();
            System.out.println("Station " + name + " unlocked");
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