public class IsCommonMultipleBranch_Mutant1 {
    public static boolean isCommonMultiple(int a, int b, int m) {
        if (m % a == 0) {
            return false;
        }
        if (m % b != 0) {
            return false;
        }
        return true;
    }
}