public class DivisionOverflow_Mutant1 {
    public static int division_test_fail_overflow(int nom, int denom) {
        int tmp = nom * denom; //change division operation to multiplication
        return tmp;
    }
}