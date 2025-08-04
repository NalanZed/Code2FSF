public class DivisionOverflow_Mutant3 {
    public static int division_test_fail_overflow(int nom, int denom) {
        int tmp = (nom+1) / denom; // modifying nom by adding 1
        return tmp;
    }
}