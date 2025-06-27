public class NotCommonFactor {

    public static boolean notCommonFactor(int a, int b, int factor) {
        return a % factor != 0 || b % factor != 0;
    }
}
