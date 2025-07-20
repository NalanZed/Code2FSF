public class GPSCheck_Mutant5 {
    public static int GPSCheck(int satellites, int signalStrength) {
        int alarm = 0;
        if (satellites < 4 && signalStrength < 20) {
            alarm = 2;
        }
        return alarm;
    }
}