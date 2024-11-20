/**
 * Container class representing the entire rail system.
 */
import java.util.*;

public class RailSystem {
    // List of all components in the system
    private final List<Component> components;
    // List of all stations in the system
    private final List<Station> stations;

    /**
     * Creates a new rail system with specified components and stations.
     * @param components List of all rail system components
     * @param stations List of all stations in the system
     */
    public RailSystem(List<Component> components, List<Station> stations) {
        this.components = new ArrayList<>(components);
        this.stations = new ArrayList<>(stations);
    }

    /**
     * @return List of all components in the rail system
     */
    public List<Component> getComponents() { return components; }

    /**
     * @return List of all stations in the rail system
     */
    public List<Station> getStations() { return stations; }
}
