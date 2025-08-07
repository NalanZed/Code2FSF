public class GPSCheck_Mutant3 {
    public static int GPSCheck(int satellites, int signalStrength) {
        int alarm = 0;
        if (satellites < 4) {
            alarm = 2;
        }
        if (signalStrength < 20) {
            alarm = 2;
        }
        return alarm;
    }
}