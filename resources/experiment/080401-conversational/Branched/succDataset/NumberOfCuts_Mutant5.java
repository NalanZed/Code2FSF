public class NumberOfCuts_Mutant5 {

    public static int numberOfCuts(int n) {
        if (n == 1) {
            return 0;
        }
        if (n % 2 == 0) {
            return n - 1; // mutated line
        }
        return n;
    }
}