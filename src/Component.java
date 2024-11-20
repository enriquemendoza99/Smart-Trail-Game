/**
 * Abstract class for all rail system components (stations, tracks,
 * switches). Each component runs on its own thread and communicates via message
 * passing.
 */
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public abstract class Component implements Runnable {
    //Queue for incoming messages to be processed by this component
    protected BlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
    //List of components directly connected to this component
    protected List<Component> neighbors = new ArrayList<>();
    // X-Y-coordinates of this component in the rail system
    protected double x, y;
    // Flag indicating if this component is currently locked by a train
    protected boolean isLocked = false;
    // Reference to the train currently occupying this component
    protected Train occupyingTrain = null;
    //Unique identifier for this component
    protected String id;
    // Counter for generating unique component IDs
    protected static int nextId = 0;

    /**
     * Creates a new component at the specified coordinates.
     * Generates a unique ID based on the component type and nextId counter.
     * @param x The x-coordinate of the component
     * @param y The y-coordinate of the component
     */
    public Component(double x, double y) {
        this.x = x;
        this.y = y;
        this.id = String.format("%s_%d", getClass().getSimpleName(), nextId++);
    }

    /** @return The x-coordinate of this component */
    public double getX() { return x; }
    /** @return The y-coordinate of this component */
    public double getY() { return y; }
    /** @return The unique identifier of this component */
    public String getId() { return id; }
    /** @return True if this component is currently locked by a train */
    public boolean isLocked() { return isLocked; }
    /** @return The train currently occupying this component, or null if none */
    public Train getOccupyingTrain() { return occupyingTrain; }

    /**
     * Adds a bidirectional connection to another component.
     * Ensures that if A is connected to B, B is also connected to A.
     * @param neighbor The component to connect with
     */
    public void addNeighbor(Component neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
            // Ensure bidirectional connection
            if (!neighbor.getNeighbors().contains(this)) {
                neighbor.addNeighbor(this);
            }
        }
    }

    /**
     * Returns a defensive copy of this component's neighbors list.
     * @return List of components connected to this component
     */
    public List<Component> getNeighbors() {
        return new ArrayList<>(neighbors);
    }

    /**
     * Sends a message to another component's inbox.
     * @param msg The message to send
     * @param target The component to receive the message
     */
    public void sendMessage(Message msg, Component target) {
        target.inbox.offer(msg);
    }

    /**
     * Main processing loop for the component.
     * Continuously processes messages from the inbox until interrupted.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message msg = inbox.take();
                processMessage(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    /**
     * Abstract method to process incoming messages.
     * Each component type must implement its own message handling logic.
     * @param msg The message to process
     */
    protected abstract void processMessage(Message msg);
    /**
     * Locks this component for use by a specific train.
     * @param train The train taking control of this component
     */
    public void lock(Train train) {
        isLocked = true;
        occupyingTrain = train;
    }
    /**
     * Unlocks this component, making it available for other trains.
     */
    public void unlock() {
        isLocked = false;
        occupyingTrain = null;
    }

    /**
     * Returns a string representation of this component.
     * @return String containing component ID and coordinates
     */
    @Override
    public String toString() {
        return id + " at (" + x + "," + y + ")";
    }
}