public interface TrainGUI {
    void updateTrain(Train train);
    void showPathError(Train train);
    void showLockError(Train train);
    void removeTrain(Train train);
    void enableOtherTrains();
    void removeTrainFromSystem(Train train);
}

