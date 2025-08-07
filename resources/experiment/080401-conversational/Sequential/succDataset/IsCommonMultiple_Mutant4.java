public class IsCommonMultiple_Mutant4 {
    public static boolean isCommonMultiple(int a, int b, int m) {
        return m % a < 0 && m % b < 0;
    }
}