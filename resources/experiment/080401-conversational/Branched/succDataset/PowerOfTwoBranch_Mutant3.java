public class PowerOfTwoBranch_Mutant3 {

    public static boolean isPowerOfTwo(int n) {
        if (n <= 0) {
            return true;
        }
        if ((n & (n - 1)) != 0) {
            return false;
        }
        return true;
    }
}