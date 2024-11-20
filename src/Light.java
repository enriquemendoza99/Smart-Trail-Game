import java.util.Set;
import java.util.HashSet;

/**
 * Represents a signal light in the rail system.
 * Controls access to track sections by maintaining a RED/GREEN state.
 * Works in conjunction with the locking mechanism to prevent collisions.
 */
public class Light extends Component {
    /**
     * Possible states for the light signal:
     * RED - Stop/Section is locked
     * GREEN - Clear to proceed/Section is available
     */
    public enum State {
        RED,
        GREEN
    }

    /** Current state of the light */
    private State state;

    /** Set of processed message IDs to prevent duplicate processing */
    private Set<String> processedMessageIds = new HashSet<>();

    /**
     * Creates a new light signal at specified coordinates.
     * Light starts in RED state by default.
     * @param x X-coordinate of the light
     * @param y Y-coordinate of the light
     */
    public Light(double x, double y) {
        super(x, y);
        this.state = State.RED;
    }

    /**
     * Processes incoming messages for the light.
     * Handles only LOCK_REQUEST and UNLOCK_REQUEST messages.
     * Implements duplicate message detection.
     * @param msg The message to process
     */
    @Override
    protected void processMessage(Message msg) {
        if (!processedMessageIds.add(msg.getMessageId())) {
            return;
        }

        switch (msg.getType()) {
            case LOCK_REQUEST:
                handleLockRequest(msg);
                break;
            case UNLOCK_REQUEST:
                handleUnlockRequest(msg);
                break;
        }
    }

    /**
     * Handles requests to lock this light.
     * Changes state to GREEN if lock is successful.
     * @param msg The lock request message
     */
    private void handleLockRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            state = State.GREEN;
            Message response = new Message(Message.Type.LOCK_RESPONSE,
                    msg.getTrain(), this, msg.getSource());
            response.setSuccess(true);
            sendMessage(response, msg.getSource());
        } else {
            Message response = new Message(Message.Type.LOCK_RESPONSE,
                    msg.getTrain(), this, msg.getSource());
            response.setSuccess(false);
            sendMessage(response, msg.getSource());
        }
    }

    /**
     * Handles requests to unlock this light.
     * Changes state back to RED when unlocked.
     * @param msg The unlock request message
     */
    private void handleUnlockRequest(Message msg) {
        if (occupyingTrain == msg.getTrain()) {
            unlock();
            state = State.RED;
        }
    }

    /**
     * @return Current state of the light (RED or GREEN)
     */
    public State getState() {
        return state;
    }
}