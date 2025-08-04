public class DivisionOverflow_Mutant5 {
    public static int division_test_fail_overflow(int nom, int denom) {
        //~should_fail
        int tmp = denom / nom; // swapping numerator and denominator
        return tmp;
    }
}