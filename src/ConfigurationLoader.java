import java.io.*;
import java.util.*;

public class ConfigurationLoader {
    private List<Component> components = new ArrayList<>();
    private List<Station> stations = new ArrayList<>();
    private Map<String, Component> componentsByLocation = new HashMap<>();

    public RailSystem loadConfiguration(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                switch (parts[0].toLowerCase()) {
                    case "station":
                        createStation(parts);
                        break;
                    case "switch":
                        createSwitch(parts);
                        break;
                    case "track":
                        createTrack(parts);
                        break;
                }
            }

            connectComponents();
            validateConfiguration();

            return new RailSystem(components, stations);
        }
    }

    private void createStation(String[] parts) throws IOException {
        if (parts.length < 3) {
            throw new IOException("Invalid station format: " + String.join(" ", parts));
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            Station station = new Station("Station_" + stations.size(), x, y);
            stations.add(station);
            components.add(station);
            componentsByLocation.put(getLocationKey(x, y), station);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid coordinates for station: " + String.join(" ", parts));
        }
    }

    private void createSwitch(String[] parts) throws IOException {
        if (parts.length < 3) {
            throw new IOException("Invalid switch format: " + String.join(" ", parts));
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            Switch sw = new Switch(x, y);
            components.add(sw);
            componentsByLocation.put(getLocationKey(x, y), sw);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid coordinates for switch: " + String.join(" ", parts));
        }
    }

    private void createTrack(String[] parts) throws IOException {
        if (parts.length < 5) {
            throw new IOException("Invalid track format: " + String.join(" ", parts));
        }

        try {
            double x1 = Double.parseDouble(parts[1]);
            double y1 = Double.parseDouble(parts[2]);
            double x2 = Double.parseDouble(parts[3]);
            double y2 = Double.parseDouble(parts[4]);
            int segments = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;

            Track track = new Track(x1, y1, x2, y2, segments);
            components.add(track);

            // Add light at track midpoint if segment length > threshold
            double length = Math.hypot(x2 - x1, y2 - y1);
            if (length > 2.0) {  // Add light for tracks longer than 2 units
                double midX = (x1 + x2) / 2;
                double midY = (y1 + y2) / 2;
                Light light = new Light(midX, midY);
                components.add(light);
            }

        } catch (NumberFormatException e) {
            throw new IOException("Invalid coordinates or segment count for track: " + String.join(" ", parts));
        }
    }

    private void connectComponents() {
        // First, connect tracks to their endpoints
        for (Component comp : components) {
            if (comp instanceof Track) {
                Track track = (Track) comp;
                Component start = findComponentAt(track.getStartX(), track.getStartY());
                Component end = findComponentAt(track.getEndX(), track.getEndY());

                if (start != null) start.addNeighbor(track);
                if (end != null) end.addNeighbor(track);
            }
        }

        // Then connect switches to their tracks
        for (Component comp : components) {
            if (comp instanceof Switch) {
                Switch sw = (Switch) comp;
                List<Component> connectedTracks = findConnectedTracks(sw);
                if (connectedTracks.size() >= 2) {
                    sw.setTracks(connectedTracks.get(0), connectedTracks.get(1));
                }
            }
        }
    }

    private Component findComponentAt(double x, double y) {
        return componentsByLocation.get(getLocationKey(x, y));
    }

    private List<Component> findConnectedTracks(Switch sw) {
        List<Component> connectedTracks = new ArrayList<>();
        for (Component comp : components) {
            if (comp instanceof Track) {
                Track track = (Track) comp;
                if (isConnected(sw, track)) {
                    connectedTracks.add(track);
                }
            }
        }
        return connectedTracks;
    }

    private boolean isConnected(Switch sw, Track track) {
        return (Math.abs(sw.getX() - track.getStartX()) < 0.01 &&
                Math.abs(sw.getY() - track.getStartY()) < 0.01) ||
                (Math.abs(sw.getX() - track.getEndX()) < 0.01 &&
                        Math.abs(sw.getY() - track.getEndY()) < 0.01);
    }

    private void validateConfiguration() throws IOException {
        if (components.size() == 0) {
            throw new IOException("Invalid configuration: no components found");
        }

        // Check for track crossings
        List<Track> tracks = new ArrayList<>();
        for (Component comp : components) {
            if (comp instanceof Track) {
                tracks.add((Track) comp);
            }
        }

        for (int i = 0; i < tracks.size(); i++) {
            Track t1 = tracks.get(i);
            for (int j = i + 1; j < tracks.size(); j++) {
                Track t2 = tracks.get(j);
                if (tracksIntersect(t1, t2)) {
                    throw new IOException("Invalid configuration: tracks cross each other");
                }
            }
        }

        // Check for dangling components
        for (Component comp : components) {
            if (comp.getNeighbors().isEmpty() && !(comp instanceof Light)) {
                throw new IOException("Invalid configuration: dangling component found");
            }
        }

        // Check for isolated stations
        for (Station station : stations) {
            if (!isStationConnected(station)) {
                throw new IOException("Invalid configuration: isolated station found");
            }
        }
    }

    private boolean tracksIntersect(Track t1, Track t2) {
        // Simple line segment intersection check
        double x1 = t1.getStartX(), y1 = t1.getStartY();
        double x2 = t1.getEndX(), y2 = t1.getEndY();
        double x3 = t2.getStartX(), y3 = t2.getStartY();
        double x4 = t2.getEndX(), y4 = t2.getEndY();

        // Calculate determinant
        double det = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(det) < 1e-10) return false;  // Lines are parallel

        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / det;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / det;

        // Check if intersection point lies on both segments
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            // Don't count intersections at shared endpoints
            double ix = x1 + t * (x2 - x1);
            double iy = y1 + t * (y2 - y1);
            return !isEndpoint(ix, iy, t1) || !isEndpoint(ix, iy, t2);
        }
        return false;
    }

    private boolean isEndpoint(double x, double y, Track track) {
        return (Math.abs(x - track.getStartX()) < 1e-10 && Math.abs(y - track.getStartY()) < 1e-10) ||
                (Math.abs(x - track.getEndX()) < 1e-10 && Math.abs(y - track.getEndY()) < 1e-10);
    }

    private boolean isStationConnected(Station station) {
        Set<Component> visited = new HashSet<>();
        Queue<Component> queue = new LinkedList<>();
        queue.add(station);
        visited.add(station);

        while (!queue.isEmpty()) {
            Component current = queue.poll();
            for (Component neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    if (neighbor instanceof Station && neighbor != station) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getLocationKey(double x, double y) {
        return String.format("%.2f,%.2f", x, y);
    }
}

class RailSystem {
    private final List<Component> components;
    private final List<Station> stations;

    public RailSystem(List<Component> components, List<Station> stations) {
        this.components = new ArrayList<>(components);
        this.stations = new ArrayList<>(stations);
    }

    public List<Component> getComponents() { return components; }
    public List<Station> getStations() { return stations; }
}