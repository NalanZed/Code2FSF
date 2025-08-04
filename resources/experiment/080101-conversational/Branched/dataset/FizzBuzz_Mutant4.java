public class FizzBuzz_Mutant4 {
    public static int fizzBuzz(int n) {
        int res = 0;
        if (n % 3 == 0) {
            res += 3;
        }
        if (n % 5 != 0) { // changed == to !=
            res += 5;
        }
        return res;
    }
}