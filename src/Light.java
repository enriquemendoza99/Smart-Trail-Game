import java.util.Set;
import java.util.HashSet;

public class Light extends Component {
    public enum State {
        RED,
        GREEN
    }

    private State state;
    private Set<String> processedMessageIds = new HashSet<>();

    public Light(double x, double y) {
        super(x, y);
        this.state = State.RED;
    }

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

    private void handleLockRequest(Message msg) {
        if (!isLocked || occupyingTrain == msg.getTrain()) {
            lock(msg.getTrain());
            state = State.GREEN;
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getSource());
            response.setSuccess(true);
            sendMessage(response, msg.getSource());
        } else {
            Message response = new Message(Message.Type.LOCK_RESPONSE, msg.getTrain(), this, msg.getSource());
            response.setSuccess(false);
            sendMessage(response, msg.getSource());
        }
    }

    private void handleUnlockRequest(Message msg) {
        if (occupyingTrain == msg.getTrain()) {
            unlock();
            state = State.RED;
        }
    }

    public State getState() {
        return state;
    }
}