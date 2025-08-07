public class SmallestEvenMulBranch_Mutant4 {

    public static int smallestEvenMultiple(int n) {
        if (n % 2 == 0) {
            return n;
        }
        return 3 * n;
    }
}