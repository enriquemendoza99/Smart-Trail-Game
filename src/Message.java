import java.util.List;

public class Message {
    public enum Type {
        FIND_PATH,
        PATH_RESPONSE,
        LOCK_REQUEST,
        LOCK_RESPONSE,
        MOVE_REQUEST,
        MOVE_COMPLETE,
        UNLOCK_REQUEST
    }

    private Type type;
    private Train train;
    private Component source;
    private Component destination;
    private List<Component> path;
    private boolean success;
    private String messageId;
    private static int nextMessageId = 0;

    public Message(Type type, Train train, Component source, Component destination) {
        this.type = type;
        this.train = train;
        this.source = source;
        this.destination = destination;
        this.messageId = "MSG_" + nextMessageId++;
    }

    public Message(Message other) {
        this.type = other.type;
        this.train = other.train;
        this.source = other.source;
        this.destination = other.destination;
        this.path = other.path;
        this.success = other.success;
        this.messageId = other.messageId;
    }

    public Type getType() { return type; }
    public Train getTrain() { return train; }
    public Component getSource() { return source; }
    public Component getDestination() { return destination; }
    public List<Component> getPath() { return path; }
    public void setPath(List<Component> path) { this.path = path; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessageId() { return messageId; }
}