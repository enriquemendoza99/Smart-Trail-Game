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
        // Avoid processing the same message multiple times
        if (!processedMessageIds.add(msg.getMessageId())) {
            System.out.println("Station " + name + " already processed message: " + msg.getMessageId());
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
            // This station is the destination, create path back to train
            List<Component> path = new ArrayList<>();
            path.add(this);
            Message response = new Message(Message.Type.PATH_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setPath(path);
            response.setSuccess(true);
            System.out.println("Station " + name + " is destination, sending path back to train");
            sendMessage(response, msg.getTrain());
        } else {
            // Forward find path request to neighbors except the source
            for (Component neighbor : neighbors) {
                if (neighbor != msg.getSource()) {
                    Message newMsg = new Message(msg);
                    System.out.println("Station " + name + " forwarding find path to " + neighbor.getId());
                    sendMessage(newMsg, neighbor);
                }
            }
        }
    }

    private void handlePathResponse(Message msg) {
        System.out.println("Station " + name + " handling path response");
        if (msg.getPath() != null) {
            // Add this station to the path
            List<Component> newPath = new ArrayList<>(msg.getPath());
            newPath.add(0, this);
            msg.setPath(newPath);
            System.out.println("Station " + name + " added to path. Path is now: " + pathToString(newPath));
        }
        // Forward the path response to the train
        sendMessage(msg, msg.getTrain());
    }

    private void handleLockRequest(Message msg) {
        System.out.println("Station " + name + " handling lock request");
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            // Lock this station for the train
            lock(msg.getTrain());

            // If this station is part of the path, forward lock request to next component
            List<Component> path = msg.getPath();
            if (path.contains(this)) {
                int currentIndex = path.indexOf(this);
                if (currentIndex >= 0 && currentIndex < path.size() - 1) {
                    Component nextInPath = path.get(currentIndex + 1);
                    Message newMsg = new Message(msg);
                    System.out.println("Station " + name + " forwarding lock request to " + nextInPath.getId());
                    sendMessage(newMsg, nextInPath);
                }
            }

            // Send success response to train
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(true);
            System.out.println("Station " + name + " sending successful lock response");
            sendMessage(response, msg.getTrain());
        } else {
            // Station is locked by another train
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(false);
            System.out.println("Station " + name + " sending failed lock response - already locked");
            sendMessage(response, msg.getTrain());
        }
    }

    private void handleMoveRequest(Message msg) {
        System.out.println("Station " + name + " handling move request");
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            // Lock this station and update train position
            lock(msg.getTrain());
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(true);
            System.out.println("Station " + name + " sending move complete");
            sendMessage(complete, msg.getTrain());
        } else {
            System.out.println("Station " + name + " cannot complete move - locked by different train");
            Message complete = new Message(Message.Type.MOVE_COMPLETE, msg.getTrain(), this, msg.getTrain());
            complete.setSuccess(false);
            sendMessage(complete, msg.getTrain());
        }
    }

    private void handleUnlockRequest(Message msg) {
        System.out.println("Station " + name + " handling unlock request");
        if (occupyingTrain == msg.getTrain()) {
            unlock();
            System.out.println("Station " + name + " unlocked");
        } else {
            System.out.println("Station " + name + " cannot unlock - locked by different train");
        }
    }

    private String pathToString(List<Component> path) {
        StringBuilder sb = new StringBuilder();
        for (Component c : path) {
            sb.append(c.getId()).append(" -> ");
        }
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}