public class PowerOfTwo_Mutant3 {

    public static boolean isPowerOfTwo(int n) {
        return n > 1 && (n & (n - 1)) == 0;
    }
}