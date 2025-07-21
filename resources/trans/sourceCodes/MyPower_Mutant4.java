public class MyPower_Mutant4 {
    public static int power(int x, int n) {
        int res = 1;
        for(int i = 0; i <= n; i++) // Loop until i is less than or equal to n instead of strictly less
	        res = res * x;
        return res;
    }
}