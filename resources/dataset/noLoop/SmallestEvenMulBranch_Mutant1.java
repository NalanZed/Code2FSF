public class SmallestEvenMulBranch_Mutant1 {

    public static int smallestEvenMultiple(int n) {
        if (n % 3 == 0) {
            return n;
        }
        return 2 * n;
    }
}