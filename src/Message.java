/**
 * Represents a message passed between components in the rail system.
 * Implements the communication protocol between components.
 */
import java.util.List;

public class Message {
    /**
     * Types of messages that can be sent between components:
     * FIND_PATH - Request to find a route to destination
     * PATH_RESPONSE - Response containing a found path
     * LOCK_REQUEST - Request to lock a component
     * LOCK_RESPONSE - Response to a lock request
     * MOVE_REQUEST - Request to move through a component
     * MOVE_COMPLETE - Notification that movement is complete
     * UNLOCK_REQUEST - Request to unlock a component
     */
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

    /**
     * Creates a new message with specified parameters.
     * Automatically generates a unique message ID.
     * @param type Type of message
     * @param train Train associated with message
     * @param source Component sending the message
     * @param destination Intended destination
     */
    public Message(Type type, Train train, Component source,
                   Component destination) {
        this.type = type;
        this.train = train;
        this.source = source;
        this.destination = destination;
        this.messageId = "MSG_" + nextMessageId++;
    }

    /**
     * Copy constructor for creating a new message based on an existing one.
     * Maintains the same messageId as the original.
     * @param other Message to copy
     */
    public Message(Message other) {
        this.type = other.type;
        this.train = other.train;
        this.source = other.source;
        this.destination = other.destination;
        this.path = other.path;
        this.success = other.success;
        this.messageId = other.messageId;
    }

    // Getter and setter methods with clear return/parameter types
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