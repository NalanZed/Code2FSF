public class IsCommonFactor_Original {

    public static boolean isCommonFactor(int a, int b, int factor) {
        return a % factor == 0 && b % factor == 0;
    }
}