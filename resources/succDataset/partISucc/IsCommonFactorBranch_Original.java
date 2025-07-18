public class IsCommonFactorBranch_Original {
    public static boolean isCommonFactor (int a, int b, int factor) {
        if (a % factor != 0) {
            return false;
        }
        if (b % factor != 0) {
            return false;
        }
        return true;
    }
}