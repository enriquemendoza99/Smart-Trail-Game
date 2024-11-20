/**
 * Configuration Loader for the SmartRail system. Responsible for reading
 * rail system configuration from a file and creating a valid rail network.
 */
import java.io.*;
import java.util.*;

public class ConfigurationLoader {
    // List of all components in the rail system
    private final List<Component> components = new ArrayList<>();
    // List of all stations in the rail system
    private final List<Station> stations = new ArrayList<>();
    // List of all tracks in the rail system
    private final List<Track> tracks = new ArrayList<>();
    // List of all switches in the rail system
    private final List<Switch> switches = new ArrayList<>();
    // Map of components indexed by their location coordinates
    private final Map<String, Component> componentsByLocation = new HashMap<>();
    // Tolerance value for floating-point coordinate comparisons
    private static final double EPSILON = 0.1;

    /**
     * Loads and validates a rail system configuration from a file.
     * @param filename Path to the configuration file
     * @return A fully configured RailSystem instance
     * @throws IOException If configuration is invalid or file cannot be read
     */
    public RailSystem loadConfiguration(String filename) throws IOException {
        loadComponents(filename);
        createConnections();
        validateConfiguration();
        printDebugInfo();
        return new RailSystem(components, stations);
    }
    /**
     * Reads components from the configuration file and creates them.
     * Each line should be in one of these formats:
     * - station x y
     * - switch x y
     * - track x1 y1 x2 y2 [segments]
     * @param filename Path to the configuration file
     * @throws IOException If file format is invalid or file cannot be read
     */
    private void loadComponents(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 3) continue;

                try {
                    switch (parts[0].toLowerCase()) {
                        case "station" -> createStation(parts);
                        case "switch" -> createSwitch(parts);
                        case "track" -> createTrack(parts);
                    }
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid number format in line: " +
                            line);
                }
            }
        }
    }

    /**
     * Creates a station from configuration file data.
     * @param parts Array containing [station, x, y]
     */
    private void createStation(String[] parts) {
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        Station station = new Station("Station_" + stations.size(), x, y);
        stations.add(station);
        components.add(station);
        componentsByLocation.put(getLocationKey(x, y), station);
        System.out.println("Created station: " + station.getId() + " at (" + x +
                "," + y + ")");
    }

    /**
     * Creates a switch from configuration file data.
     * @param parts Array containing [switch, x, y]
     */
    private void createSwitch(String[] parts) {
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        Switch sw = new Switch(x, y);
        switches.add(sw);
        components.add(sw);
        componentsByLocation.put(getLocationKey(x, y), sw);
        System.out.println("Created switch: " + sw.getId() + " at (" + x + "," +
                y + ")");
    }

    /**
     * Creates a track from configuration file data.
     * @param parts Array containing [track, x1, y1, x2, y2, segments(optional)]
     * @throws IllegalArgumentException If track parameters are invalid
     */
    private void createTrack(String[] parts) {
        if (parts.length < 5) {
            throw new IllegalArgumentException("Invalid track format: " +
                    String.join(" ", parts));
        }
        double x1 = Double.parseDouble(parts[1]);
        double y1 = Double.parseDouble(parts[2]);
        double x2 = Double.parseDouble(parts[3]);
        double y2 = Double.parseDouble(parts[4]);
        int segments = parts.length > 5 ? Integer.parseInt(parts[5]) : 1;
        if (Math.abs(x2 - x1) < EPSILON && Math.abs(y2 - y1) < EPSILON) {
            throw new IllegalArgumentException("Zero-length track detected");
        }
        Track track = new Track(x1, y1, x2, y2, segments);
        tracks.add(track);
        components.add(track);
        System.out.println("Created track: " + track.getId() + " from (" + x1 +
                "," + y1 + ") to (" + x2 + "," + y2 + ")");
    }

    /**
     * Creates all necessary connections between components.
     * Performs connection in three phases:
     * 1. Connect tracks to their endpoints
     * 2. Connect tracks that share endpoints
     * 3. Configure switches with their connected tracks
     */
    private void createConnections() {
        for (Track track : tracks) {
            connectTrackEndpoints(track);
        }
        for (int i = 0; i < tracks.size(); i++) {
            for (int j = i + 1; j < tracks.size(); j++) {
                connectTracksIfShared(tracks.get(i), tracks.get(j));
            }
        }
        for (Switch sw : switches) {
            configureSwitch(sw);
        }
    }
    /**
     * Connects a track to its endpoint components and any switches along its
     * length.
     * @param track The track to connect
     */
    private void connectTrackEndpoints(Track track) {
        for (Component comp : components) {
            if (comp instanceof Track) continue;

            if (isNearPoint(comp.getX(), comp.getY(), track.getStartX(),
                    track.getStartY()) ||
                    isNearPoint(comp.getX(), comp.getY(), track.getEndX(),
                            track.getEndY())) {
                connect(track, comp);
            }
        }
        for (Switch sw : switches) {
            if (isPointOnTrack(sw.getX(), sw.getY(), track)) {
                connect(track, sw);
            }
        }
    }

    /**
     * Connects two tracks if they share an endpoint.
     * @param t1 First track to check
     * @param t2 Second track to check
     */
    private void connectTracksIfShared(Track t1, Track t2) {
        if (tracksShare(t1, t2)) {
            connect(t1, t2);
        }
    }

    /**
     * Configures a switch with its main and alternate tracks.
     * Main track is determined by finding the most horizontal track.
     * @param sw The switch to configure
     */
    private void configureSwitch(Switch sw) {
        // Stream method is used to get a Sequential Stream from the array
        // passed as the parameter with its elements.
        List<Track> connectedTracks = sw.getNeighbors().stream()
                .filter(c -> c instanceof Track)
                .map(c -> (Track) c)
                .distinct()
                .toList();

        if (connectedTracks.size() >= 2) {
            Track mainTrack = findMainTrack(connectedTracks);
            Track altTrack = connectedTracks.stream()
                    .filter(t -> t != mainTrack)
                    .findFirst()
                    .orElse(mainTrack);

            sw.setTracks(mainTrack, altTrack);
            System.out.println("Configured switch " + sw.getId() +
                    " with main=" + mainTrack.getId() + ", alt=" +
                    altTrack.getId());
        }
    }
    /**
     * Finds the most horizontal track from a list of tracks.
     * Determines this by comparing the absolute slopes of the tracks.
     * @param tracks List of tracks to evaluate
     * @return Track with the smallest absolute slope (most horizontal)
     */
    private Track findMainTrack(List<Track> tracks) {
        // Stream method is used to get a Sequential Stream from the array
        // passed as the parameter with its elements.
        return tracks.stream()
                .min((t1, t2) -> {
                    double slope1 = Math.abs((t1.getEndY() -
                            t1.getStartY()) / (t1.getEndX() - t1.getStartX()));
                    double slope2 = Math.abs((t2.getEndY() -
                            t2.getStartY()) / (t2.getEndX() - t2.getStartX()));
                    return Double.compare(slope1, slope2);
                })
                .orElse(tracks.get(0));
    }

    /**
     * Creates a bidirectional connection between two components.
     * @param c1 First component to connect
     * @param c2 Second component to connect
     */
    private void connect(Component c1, Component c2) {
        if (!c1.getNeighbors().contains(c2)) {
            c1.addNeighbor(c2);
            c2.addNeighbor(c1);
            System.out.println("Connected: " + c1.getId() + " <-> " +
                    c2.getId());
        }
    }

    /**
     * Checks if two tracks share any endpoints.
     * @param t1 First track to check
     * @param t2 Second track to check
     * @return true if tracks share any endpoint
     */
    private boolean tracksShare(Track t1, Track t2) {
        return isNearPoint(t1.getStartX(), t1.getStartY(),
                t2.getStartX(), t2.getStartY()) ||
                isNearPoint(t1.getStartX(), t1.getStartY(),
                        t2.getEndX(), t2.getEndY()) ||
                isNearPoint(t1.getEndX(), t1.getEndY(),
                        t2.getStartX(), t2.getStartY()) ||
                isNearPoint(t1.getEndX(), t1.getEndY(),
                        t2.getEndX(), t2.getEndY());
    }
    /**
     * Checks if a point lies on a track segment.
     * Uses a bounding box check followed by a cross product calculation
     * to determine if the point lies on the line defined by the track.
     * @param px X-coordinate of point to check
     * @param py Y-coordinate of point to check
     * @param track Track to check against
     * @return true if the point lies on the track
     */
    private boolean isPointOnTrack(double px, double py, Track track) {
        double x1 = track.getStartX(), y1 = track.getStartY();
        double x2 = track.getEndX(), y2 = track.getEndY();

        // Check if point is within track's bounding box
        if (px < Math.min(x1, x2) - EPSILON || px > Math.max(x1, x2) +
                EPSILON ||
                py < Math.min(y1, y2) - EPSILON || py > Math.max(y1, y2) +
                EPSILON) {
            return false;
        }

        // Check if point lies on the track line
        double crossProduct = Math.abs((y2 - y1) * (px - x1) -
                (x2 - x1) * (py - y1));
        double lineLength = Math.hypot(x2 - x1, y2 - y1);
        return crossProduct / lineLength < EPSILON;
    }

    /**
     * Checks if two points are within EPSILON distance of each other.
     * @param x1 First point X coordinate
     * @param y1 First point Y coordinate
     * @param x2 Second point X coordinate
     * @param y2 Second point Y coordinate
     * @return true if points are considered to be at the same location
     */
    private boolean isNearPoint(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1) < EPSILON;
    }

    /**
     * Validates the entire configuration.
     * @throws IOException if any validation fails
     */
    private void validateConfiguration() throws IOException {
        validateSwitchConnections();
        validateTrackCrossings();
        validateStationConnectivity();
    }
    /**
     * Validates that all switches have at least two track connections.
     * @throws IOException if a switch has insufficient connections
     */
    private void validateSwitchConnections() throws IOException {
        for (Switch sw : switches) {
            long trackConnections = sw.getNeighbors().stream()
                    .filter(c -> c instanceof Track)
                    .count();

            if (trackConnections < 2) {
                throw new IOException("Switch at (" + sw.getX() + "," +
                        sw.getY() + ") has insufficient connections: " +
                        trackConnections);
            }
        }
    }
    /**
     * Validates that no tracks cross each other except at endpoints.
     * @throws IOException if any invalid track crossing is found
     */
    private void validateTrackCrossings() throws IOException {
        for (int i = 0; i < tracks.size(); i++) {
            for (int j = i + 1; j < tracks.size(); j++) {
                Track t1 = tracks.get(i);
                Track t2 = tracks.get(j);
                if (tracksIntersect(t1, t2)) {
                    throw new IOException("Invalid track crossing between " +
                            t1.getId() + " and " + t2.getId());
                }
            }
        }
    }

    /**
     * Validates that all stations are connected to at least one other station.
     * @throws IOException if any isolated station is found
     */
    private void validateStationConnectivity() throws IOException {
        for (Station station : stations) {
            if (!isStationConnected(station)) {
                throw new IOException("Isolated station found at (" +
                        station.getX() + "," + station.getY() + ")");
            }
        }
    }

    /**
     * Checks if two tracks intersect at any point other than their endpoints.
     * @param t1 First track to check
     * @param t2 Second track to check
     * @return true if tracks intersect
     */
    private boolean tracksIntersect(Track t1, Track t2) {
        if (tracksShare(t1, t2)) return false;

        double x1 = t1.getStartX(), y1 = t1.getStartY();
        double x2 = t1.getEndX(), y2 = t1.getEndY();
        double x3 = t2.getStartX(), y3 = t2.getStartY();
        double x4 = t2.getEndX(), y4 = t2.getEndY();

        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < EPSILON) return false;

        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;

        return ua > EPSILON && ua < 1 - EPSILON && ub > EPSILON && ub < 1 -
                EPSILON;
    }

    /**
     * Checks if a station is connected to at least one other station
     * through the rail network.
     * @param start Station to check
     * @return true if station is connected to at least one other station
     */
    private boolean isStationConnected(Station start) {
        Set<Component> visited = new HashSet<>();
        Queue<Component> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Component current = queue.poll();
            for (Component neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                    if (neighbor instanceof Station && neighbor != start) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Creates a unique key for a location in the rail system.
     * @param x X-coordinate
     * @param y Y-coordinate
     * @return String key representing the location
     */
    private String getLocationKey(double x, double y) {
        return String.format("%.2f,%.2f", x, y);
    }

    /**
     * Prints debug information about the configuration.
     */
    private void printDebugInfo() {
        System.out.println("\nConfiguration Summary:");
        System.out.println("Stations: " + stations.size());
        System.out.println("Switches: " + switches.size());
        System.out.println("Tracks: " + tracks.size());

        System.out.println("\nComponent Connections:");
        for (Component comp : components) {
            System.out.println(comp.getId() + " [" +
                    comp.getClass().getSimpleName() +
                    "] is connected to: " +
                    comp.getNeighbors().stream()
                    .map(n -> n.getId() + "[" + n.getClass().getSimpleName() +
                            "]")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("none"));
        }
    }
}