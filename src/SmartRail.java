public class SmartRail {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java SmartRail <config-file>");
            System.out.println("Example: java SmartRail simple.txt");
            System.exit(1);
        }

        SmartRailGUI.main(args);
    }
}
