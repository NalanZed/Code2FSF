public class NumberOfCuts_Mutant2 {

    public static int numberOfCuts(int n) {
        if (n == 1) {
            return 0;
        }
        if (n % 2 == 1) { // mutated line
            return n / 2;
        }
        return n;
    }
}