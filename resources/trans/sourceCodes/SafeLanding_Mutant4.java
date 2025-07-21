public class SafeLanding_Mutant4 {
    public static boolean safeLanding(int height, int speed, int tilt) {
        boolean safe = true;
        if (height > 5) {
            safe = false;
        }
        if (speed > 3) {
            safe = false;
        }
        if (tilt > 10) {
            safe = false;
        }
        return safe;
    }
}