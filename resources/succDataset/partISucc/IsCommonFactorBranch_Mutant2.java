public class IsCommonFactorBranch_Mutant2 {
    public static boolean isCommonFactor (int a, int b, int factor) {
        if (a % factor != 0) {
            return true;
        }
        if (b % factor != 0) {
            return true;
        }
        return false;
    }
}