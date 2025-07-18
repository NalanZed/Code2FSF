public class IsCommonFactorBranch_Mutant3 {
    public static boolean isCommonFactor (int a, int b, int factor) {
        if (a % factor != 1) {
            return false;
        }
        if (b % factor != 1) {
            return false;
        }
        return true;
    }
}