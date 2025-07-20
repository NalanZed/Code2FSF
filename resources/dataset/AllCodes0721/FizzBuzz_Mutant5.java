public class FizzBuzz_Mutant5 {
    public static int fizzBuzz(int n) {
        int res = 0;
        if (n % 3 == 0) {
            res += 3;
        }
        else if (n % 5 == 0) { // changed from second if to else if
            res += 5;
        }
        return res;
    }
}