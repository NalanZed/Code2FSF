public class SmallestEvenMulBranch_Mutant3 {

    public static int smallestEvenMultiple(int n) {
        if (n % 2 != 0) {
            return n;
        }
        return 2 * n;
    }
}