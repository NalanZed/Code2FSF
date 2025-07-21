public class PrimeCheck_Mutant5 {
    public static boolean isPrime(int a) {
        int i = 2;
        int mid = a / 2;
        while (i <= mid) {
            if (a % i == 0)
                return true;
            i++;
        }
        return false;
    }
}