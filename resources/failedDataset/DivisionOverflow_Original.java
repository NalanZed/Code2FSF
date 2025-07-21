public class DivisionOverflow_Original {

    static int division_test_fail_overflow(int nom, int denom) //@ requires denom != 0;
    //@ ensures result == nom / denom;
    {
        //~ should_fail
        int tmp = nom / denom;
        //~allow_dead_code
        return tmp;
    }
}