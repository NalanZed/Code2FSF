public class ReLU_Mutant2 {
    public static double computeReLU(double x) {
        if(x >= 0.0) {
            return x;
        }
        return 1.0;
    }
}