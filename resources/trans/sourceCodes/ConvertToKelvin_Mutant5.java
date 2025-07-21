public class ConvertToKelvin_Mutant5 {
    public static double convertTemperature(double celsius) {
        if(celsius < 0.0) { // Added a conditional statement
            return 0;
        }
        return celsius + 273.15;
    }
}