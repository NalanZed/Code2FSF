public class PowerOfTwoLoop_Mutant2 {

    public static boolean isPowerOfTwo(int n) {
        if (n <= 0) {
            return true;
        }
        while (n % 2 == 0) {
            n /= 2;
        }
        return n == 1;
    }
}