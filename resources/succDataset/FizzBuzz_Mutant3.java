public class FizzBuzz_Mutant3 {
    public static int fizzBuzz(int n) {
        int res = 0;
        if (n % 2 == 0) { // changed from 3 to 2
            res += 3;
        }
        if (n % 5 == 0) {
            res += 5;
        }
        return res;
    }
}