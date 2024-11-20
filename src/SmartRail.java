/**
 * Main entry point for the SmartRail application.
 * Handles command-line argument validation and launches the GUI.
 */
public class SmartRail {
    /**
     * Main method that starts the application.
     * @param args Command line arguments. args[0] should be the config
     *             file path
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SmartRail <config-file>");
            System.out.println("Example: java SmartRail simple.txt");
            System.exit(1);
        }
        SmartRailGUI.main(args);
    }
}
