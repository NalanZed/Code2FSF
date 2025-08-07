public class IsCommonMultipleBranch_Mutant2 {
    public static boolean isCommonMultiple(int a, int b, int m) {
        if (m % a != 0) {
            return true;
        }
        if (m % b != 0) {
            return false;
        }
        return true;
    }
}