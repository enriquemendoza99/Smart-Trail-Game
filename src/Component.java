import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public abstract class Component implements Runnable {
    protected BlockingQueue<Message> inbox = new LinkedBlockingQueue<>();
    protected List<Component> neighbors = new ArrayList<>();
    protected double x, y;
    protected boolean isLocked = false;
    protected Train occupyingTrain = null;
    protected String id;
    protected static int nextId = 0;

    public Component(double x, double y) {
        this.x = x;
        this.y = y;
        this.id = getClass().getSimpleName() + "_" + nextId++;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public String getId() { return id; }
    public boolean isLocked() { return isLocked; }
    public Train getOccupyingTrain() { return occupyingTrain; }

    public void addNeighbor(Component neighbor) {
        if (!neighbors.contains(neighbor)) {
            neighbors.add(neighbor);
            neighbor.addNeighbor(this);
        }
    }

    public void sendMessage(Message msg, Component target) {
        target.inbox.offer(msg);
    }

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

    protected abstract void processMessage(Message msg);

    public void lock(Train train) {
        isLocked = true;
        occupyingTrain = train;
    }

    public void unlock() {
        isLocked = false;
        occupyingTrain = null;
    }

    public List<Component> getNeighbors() {
        return new ArrayList<>(neighbors);
    }
}