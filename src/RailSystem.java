import java.util.*;

public class RailSystem {
    private final List<Component> components;
    private final List<Station> stations;

    public RailSystem(List<Component> components, List<Station> stations) {
        this.components = new ArrayList<>(components);
        this.stations = new ArrayList<>(stations);
    }

    public List<Component> getComponents() { return components; }
    public List<Station> getStations() { return stations; }
}
