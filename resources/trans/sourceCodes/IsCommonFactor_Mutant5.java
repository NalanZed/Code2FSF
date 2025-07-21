public class IsCommonFactor_Mutant5 {

    public static boolean isCommonFactor(int a, int b, int factor) {
        return a % factor == 0 && b / factor == 0; // changed '%' to '/' at b % factor
    }
}