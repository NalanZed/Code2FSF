public class Geofence_Mutant2 {
    public static int geofence(int x, int y, int limitX, int limitY) {
        int alarm = 0;
        if (x < 0 || x > limitX) {
            alarm = 1;
        }
        if (y <= 0 || y >= limitY) {
            alarm = 1;
        }
        return alarm;
    }
}