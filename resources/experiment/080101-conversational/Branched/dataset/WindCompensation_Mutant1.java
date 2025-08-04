public class WindCompensation_Mutant1 {
    public static int windCompensation(int windSpeed) {
        int compensation = 0;
        if (windSpeed > 20) {
            compensation = (windSpeed - 20) * 3;
        } else if (windSpeed > 10) {
            compensation = windSpeed - 10;
        }
        return compensation;
    }
}