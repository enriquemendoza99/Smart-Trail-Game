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
        System.out.println("Created Track: " + getId() + " from (" + startX + "," + startY + ") to (" + endX + "," + endY + ")");
    }

    @Override
    protected void processMessage(Message msg) {
        if (!processedMessageIds.add(msg.getMessageId())) {
            System.out.println("Track " + getId() + " already processed message: " + msg.getMessageId());
            return;
        }

        System.out.println("Track " + getId() + " processing message: " + msg.getType());
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
        System.out.println("Track " + getId() + " handling find path request");
        for (Component neighbor : neighbors) {
            if (neighbor != msg.getSource()) {
                Message newMsg = new Message(msg);
                System.out.println("Track " + getId() + " forwarding find path to: " + neighbor.getId());
                sendMessage(newMsg, neighbor);
            }
        }
    }

    private void handlePathResponse(Message msg) {
        System.out.println("Track " + getId() + " handling path response");
        if (msg.getPath() != null) {
            List<Component> newPath = new ArrayList<>(msg.getPath());
            newPath.add(0, this);
            msg.setPath(newPath);
            System.out.println("Track " + getId() + " added to path. Path is now: " + pathToString(newPath));
        }
        sendMessage(msg, msg.getTrain());
    }

    private void handleLockRequest(Message msg) {
        System.out.println("Track " + getId() + " handling lock request");
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            System.out.println("Track " + getId() + " locked for train: " + msg.getTrain().getId());

            List<Component> path = msg.getPath();
            if (path.contains(this)) {
                int currentIndex = path.indexOf(this);
                if (currentIndex >= 0 && currentIndex < path.size() - 1) {
                    Component nextInPath = path.get(currentIndex + 1);
                    Message newMsg = new Message(msg);
                    System.out.println("Track " + getId() + " forwarding lock request to: " + nextInPath.getId());
                    sendMessage(newMsg, nextInPath);
                }
            }

            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(true);
            sendMessage(response, msg.getTrain());
        } else {
            System.out.println("Track " + getId() + " cannot lock - already locked by different train");
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getTrain());
            response.setSuccess(false);
            sendMessage(response, msg.getTrain());
        }
    }

    private void handleMoveRequest(Message msg) {
        System.out.println("Track " + getId() + " handling move request");
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());

            Thread moveThread = new Thread(() -> {
                try {
                    // Number of steps and delay for visible movement
                    int steps = 20;  // Number of intermediate positions
                    long delay = 200; // Milliseconds between updates (slower movement)

                    // Move the train step by step
                    for (int i = 0; i <= steps; i++) {
                        final double progress = (double) i / steps;
                        // Calculate new position
                        msg.getTrain().x = startX + (endX - startX) * progress;
                        msg.getTrain().y = startY + (endY - startY) * progress;
                        msg.getTrain().updateGUI();
                        Thread.sleep(delay);  // Longer delay for visibility
                    }

                    // Send move complete message
                    Message complete = new Message(Message.Type.MOVE_COMPLETE,
                            msg.getTrain(),
                            this,
                            msg.getTrain());
                    complete.setSuccess(true);
                    sendMessage(complete, msg.getTrain());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            moveThread.setDaemon(true);
            moveThread.start();
        }
    }

    private void handleUnlockRequest(Message msg) {
        System.out.println("Track " + getId() + " handling unlock request");
        if (occupyingTrain == msg.getTrain()) {
            unlock();
            System.out.println("Track " + getId() + " unlocked");
        } else {
            System.out.println("Track " + getId() + " cannot unlock - locked by different train");
        }
    }

    private String pathToString(List<Component> path) {
        StringBuilder sb = new StringBuilder();
        for (Component c : path) {
            sb.append(c.getId()).append(" -> ");
        }
        return sb.toString();
    }

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
    public int getSegments() { return segments; }

    @Override
    public String toString() {
        return "Track " + getId() + " [(" + startX + "," + startY + ") to (" + endX + "," + endY + ")]";
    }
}