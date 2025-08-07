public class SmallestEvenMul_Original {

    public static int smallestEvenMultiple(int n) {
        return n % 2 == 0 ? n : 2 * n;
    }
}