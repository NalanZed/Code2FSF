public class NumberOfCuts_Mutant4 {

    public static int numberOfCuts(int n) {
        if (n == 2) { // mutated line
            return 0; 
        }
        if (n % 2 == 0) {
            return n / 2;
        }
        return n;
    }
}