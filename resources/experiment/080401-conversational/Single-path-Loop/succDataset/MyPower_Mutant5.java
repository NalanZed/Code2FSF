public class MyPower_Mutant5 {
    public static int power(int x, int n) {
        int res = 1;
        for(int i = 0; i < n; i++){
	        res = res * x;
        }
        return res - 1;  // Subtract 1 from the result
    }
}