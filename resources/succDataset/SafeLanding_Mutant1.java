public class SafeLanding_Mutant1 {
    public static int safeLanding(int height, int speed, int tilt) {
        int safe = 1;
        if (height <= 5) {
            safe = 0;
        }
        if (speed > 3) {
            safe = 0;
        }
        if (tilt > 10) {
            safe = 0;
        }
        return safe;
    }
}