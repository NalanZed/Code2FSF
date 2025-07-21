public class PrimeCheck_Mutant4 {
    public static boolean isPrime(int a) {
        int i = 2;
        int mid = a / 3;
        while (i <= mid) {
            if (a % i == 0)
                return false;
            i++;
        }
        return true;
    }
}