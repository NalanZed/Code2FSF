public class IsCommonFactorBranch_Mutant1 {
    public static boolean isCommonFactor (int a, int b, int factor) {
        if (a % factor == 0) {
            return false;
        }
        if (b % factor == 0) {
            return false;
        }
        return true;
    }
}