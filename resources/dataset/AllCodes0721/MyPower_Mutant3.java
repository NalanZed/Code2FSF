public class MyPower_Mutant3 {
    public static int power(int x, int n) {
        int res = 1;
        for(int i = 0; i < n; i++)
	        res = res + x;  // Use addition instead of multiplication
        return res;
    }
}