public class SafeLanding_Mutant5 {
    public static int safeLanding(int height, int speed, int tilt) {
        int safe = 1;
        if (height > 5 || speed > 3 || tilt > 10) {
            safe = 0;
        }
        return safe;
    }
}