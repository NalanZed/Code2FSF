public class SmallestEvenMulBranch_Mutant2 {

    public static int smallestEvenMultiple(int n) {
        if (n % 2 == 0) {
            return n + 1;
        }
        return 2 * n;
    }
}