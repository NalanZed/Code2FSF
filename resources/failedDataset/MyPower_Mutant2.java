public class MyPower_Mutant2 {
    public static int power(int x, int n) {
        int res = 1;
        for(int i = 1; i <= n; i++) // Start the loop from 1 rather than 0
	        res = res * x;
        return res;
    }
}