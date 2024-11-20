/**
 * Interface defining GUI functionality for train visualization and control.
 * Provides methods for updating train display and handling system events.
 */
public interface TrainGUI {
    /**
     * Updates the visual representation of a train.
     * @param train Train to update
     */
    void updateTrain(Train train);

    /**
     * Shows error when path finding fails.
     * @param train Train that encountered the error
     */
    void showPathError(Train train);

    /**
     * Shows error when path locking fails.
     * @param train Train that encountered the error
     */
    void showLockError(Train train);


    /**
     * Removes a train from display.
     * @param train Train to remove
     */
    void removeTrain(Train train);

    /**
     * Enables UI controls for adding new trains.
     */
    void enableOtherTrains();

    /**
     * Removes a train from system and updates display.
     * @param train Train to remove
     */
    void removeTrainFromSystem(Train train);
}

