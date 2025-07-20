public class FizzBuzz_Mutant1 {
    public static int fizzBuzz(int n) {
        int res = 0;
        if (n % 3 == 0) {
            res += 5; // change from 3 to 5
        }
        if (n % 5 == 0) {
            res += 5;
        }
        return res;
    }
}