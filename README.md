# SmartRail — Multi-threaded Rail Simulation

A Java rail traffic simulation where autonomous trains navigate a network
of stations, tracks, and switches using message-passing concurrency.
Each component runs on its own thread and communicates exclusively
through messages, modeling a distributed signaling system.

## Project Structure
src/

Component.java           — Abstract base class for all rail components

ConfigurationLoader.java — Parses and validates config files

Light.java                — Signal light component

Message.java              — Message types for inter-component communication

RailSystem.java            — Container for the loaded rail network

SmartRail.java              — Application entry point

SmartRailGUI.java            — JavaFX GUI and rendering

Station.java                  — Station component

Switch.java                    — Switch/turnout component

Track.java                      — Track segment component

Train.java                       — Train component with path finding

TrainGUI.java                     — Interface for GUI callbacks

example_config/

simple.txt, sample.txt, single_switch.txt,

branch_rejoin.txt, broken_crossing.txt,

simple2.txt, simple_parallel.txt, uneven.txt

doc/

Design Project 4.pdf

## How to Run

**With JavaFX SDK (required, not bundled with the JDK):**
cd src

java --module-path "path\to\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml -cp ../out/production/<module-name> SmartRail ../example_config/simple.txt

Replace `path\to\javafx-sdk-21` with your JavaFX SDK location and
`<module-name>` with your IntelliJ output folder name.

## How to Use

1. Launch the simulation with a configuration file
2. The rail network renders on the canvas — stations (green squares),
   switches (orange circles), and tracks (black lines)
3. Select a **Source Station** and **Destination Station** from the dropdowns
4. Click **Add Train** to launch a new train
5. Watch the train's status change color as it seeks a path, locks the
   route, and moves to its destination
6. Once the train arrives, the controls re-enable for the next train

## Color Legend
- Station / Switch / Track: green/orange/black when free, red when locked
- Train Idle: gray
- Train Seeking Path: yellow
- Train Locking Path: purple
- Train Moving: green

## Configuration File Format
station x y

switch x y

track x1 y1 x2 y2 [segments]
Lines starting with `#` are treated as comments.

## File Manifest

1. `SmartRail.java` — Entry point, validates arguments and launches the GUI
2. `SmartRailGUI.java` — JavaFX application, canvas rendering, train controls
3. `Component.java` — Abstract base for all rail components with threading
   and message inbox
4. `Message.java` — Typed messages for path finding, locking, and movement
5. `Station.java` — Endpoint component handling path requests and locking
6. `Track.java` — Track segment with animated train movement
7. `Switch.java` — Turnout component routing between main and alt tracks
8. `Light.java` — Signal light component (RED/GREEN state)
9. `Train.java` — Autonomous train with path finding, locking, and movement
   state machine
10. `ConfigurationLoader.java` — Parses config files and validates the network
11. `RailSystem.java` — Container for loaded components and stations
12. `TrainGUI.java` — Interface decoupling Train logic from GUI updates
