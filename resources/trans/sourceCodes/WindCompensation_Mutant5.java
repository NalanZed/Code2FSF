public class WindCompensation_Mutant5 {
    public static int windCompensation(int windSpeed) {
        int compensation = 5;
        if (windSpeed > 20) {
            compensation += (windSpeed - 20) * 2;
        } else if (windSpeed > 10) {
            compensation += windSpeed - 10;
        }
        return compensation;
    }
}