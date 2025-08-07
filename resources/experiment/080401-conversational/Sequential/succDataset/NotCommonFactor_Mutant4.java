public class NotCommonFactor_Mutant4 {

    public static boolean notCommonFactor(int a, int b, int factor) {
        return a % factor != 0 || b % factor != 1; // Changed 0 to 1 in second condition
    }
}