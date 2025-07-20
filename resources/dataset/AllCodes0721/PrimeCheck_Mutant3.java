public class PrimeCheck_Mutant3 {
    public static boolean isPrime(int a) {
        int i = 2;
        int mid = a / 2;
        while (i <= mid) {
            if (a % i == 0)
                return false;
            i += 2;
        }
        return true;
    }
}