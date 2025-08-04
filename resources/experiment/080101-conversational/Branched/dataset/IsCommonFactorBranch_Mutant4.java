public class IsCommonFactorBranch_Mutant4 {
    public static boolean isCommonFactor (int a, int b, int factor) {
        if (a % factor != 0) {
            return false;
        }
        return true;
    }
}