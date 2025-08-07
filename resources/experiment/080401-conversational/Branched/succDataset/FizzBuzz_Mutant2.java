public class FizzBuzz_Mutant2 {
    public static int fizzBuzz(int n) {
        int res = 1; // changed from 0 to 1
        if (n % 3 == 0) {
            res += 3;
        }
        if (n % 5 == 0) {
            res += 5;
        }
        return res;
    }
}