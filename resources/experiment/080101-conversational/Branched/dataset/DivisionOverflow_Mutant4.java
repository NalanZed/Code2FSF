public class DivisionOverflow_Mutant4 {
    public static int division_test_fail_overflow(int nom, int denom) {
        //~should_fail
        if (denom != 0) { //add a check for denominator to be non-zero
            int tmp = nom / denom;
            return tmp;
        }
        return 0;
    }
}