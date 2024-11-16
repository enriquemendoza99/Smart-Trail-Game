import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class Track extends Component {
    private final double startX, startY, endX, endY;
    private final int segments;
    private Set<String> processedMessageIds = new HashSet<>();
    private static final long MOVE_DELAY = 3000; // 3 seconds per track segment
    private static final int MOVEMENT_STEPS = 50; // More steps for smoother movement

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
            System.out.println("Track " + getId() + " added self to path. New path size: " + newPath.size());
        }
        sendMessage(msg, msg.getDestination());
    }

    private void handleLockRequest(Message msg) {
        System.out.println("Track " + getId() + " handling lock request");
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            System.out.println("Track " + getId() + " locked for train: " + msg.getTrain().getId());
            for (Component neighbor : neighbors) {
                if (msg.getPath().contains(neighbor)) {
                    Message newMsg = new Message(msg);
                    System.out.println("Track " + getId() + " forwarding lock request to: " + neighbor.getId());
                    sendMessage(newMsg, neighbor);
                }
            }
            // Send successful lock response
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

    private void handleLockResponse(Message msg) {
        System.out.println("Track " + getId() + " forwarding lock response to train");
        sendMessage(msg, msg.getTrain());
    }

    private void handleMoveRequest(Message msg) {
        System.out.println("Track " + getId() + " handling move request");
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            System.out.println("Track " + getId() + " starting movement animation");

            Thread moveThread = new Thread(() -> {
                try {
                    // Much slower, smoother movement
                    for (int i = 0; i <= MOVEMENT_STEPS; i++) {
                        final double progress = (double) i / MOVEMENT_STEPS;
                        msg.getTrain().x = startX + (endX - startX) * progress;
                        msg.getTrain().y = startY + (endY - startY) * progress;
                        Thread.sleep(MOVE_DELAY / MOVEMENT_STEPS);
                        msg.getTrain().updateGUI(); // Update GUI at each step
                    }

                    System.out.println("Track " + getId() + " completed movement");
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

    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
    public int getSegments() { return segments; }

    @Override
    public String toString() {
        return "Track " + getId() + " [(" + startX + "," + startY + ") to (" + endX + "," + endY + ")]";
    }

    @Override
    public void lock(Train train) {
        System.out.println("Track " + getId() + " being locked by train: " + train.getId());
        super.lock(train);
    }

    @Override
    public void unlock() {
        System.out.println("Track " + getId() + " being unlocked");
        super.unlock();
    }
}
