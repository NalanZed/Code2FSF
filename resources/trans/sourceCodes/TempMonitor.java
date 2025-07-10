public class TempMonitor {
    public static int tempMonitor(int temp) {
        int alarm = 0;
        if (temp > 100) {
            alarm = 3;
        } else if (temp > 90) {
            alarm = 2;
        } else if (temp > 80) {
            alarm = 1;
        }
        return alarm;
    }
}