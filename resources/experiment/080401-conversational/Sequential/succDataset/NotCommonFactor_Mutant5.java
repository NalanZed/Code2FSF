public class NotCommonFactor_Mutant5 {

    public static boolean notCommonFactor(int a, int b, int factor) {
        return a % factor != 1 || b % factor != 0; // Changed 0 to 1 in first condition
    }
}