public class ReLU_Mutant4 {
    public static double computeReLU(double x) {
        if(x >= 0.0) {
            return -x;
        }
        return 0.0;
    }
}