public class ReLU {
    //T : x >= 0, D: output == x , result == x, return_value == x
    public static double computeReLU(double x) {
        if(x >= 0) {
            return x;
        }
        return 0.0;
    }
}