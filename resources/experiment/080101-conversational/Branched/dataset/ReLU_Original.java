public class ReLU_Original {
    public static double computeReLU(double x) {
        if(x >= 0.0) {
            return x;
        }
        return 0.0;
    }
}