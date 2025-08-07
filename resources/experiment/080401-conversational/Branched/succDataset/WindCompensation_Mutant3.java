public class WindCompensation_Mutant3 {
    public static int windCompensation(int windSpeed) {
        int compensation = 0;
        if (windSpeed > 20) {
            compensation = (windSpeed - 20) * 2;
        } else if (windSpeed > 10) {
            compensation = (windSpeed - 10) * 2;
        }
        return compensation;
    }
}