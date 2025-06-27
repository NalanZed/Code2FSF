public class NotCommonFactorBranch {

    public static boolean notCommonFactor(int a, int b, int factor) {
        if (a % factor != 0) {
            return true;
        }
        if (b % factor != 0) {
            return true;
        }
        return false;
    }
}
