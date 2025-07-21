public class NumberOfCuts_Mutant3 {

    public static int numberOfCuts(int n) {
        if (n == 1) {
            return 0;
        }
        if (n % 2 != 0) { // mutated line
            return n / 2; 
        }
        return n;
    }
}