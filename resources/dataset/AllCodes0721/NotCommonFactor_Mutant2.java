public class NotCommonFactor_Mutant2 {

    public static boolean notCommonFactor(int a, int b, int factor) {
        return a % factor != 0 && b % factor != 0; // Changed || to &&
    }
}