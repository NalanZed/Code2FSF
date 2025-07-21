public class NumberOfCuts_Mutant1 {

    public static int numberOfCuts(int n) {
        if (n == 1) {
            return 1; // mutated line
        }
        if (n % 2 == 0) {
            return n / 2;
        }
        return n;
    }
}