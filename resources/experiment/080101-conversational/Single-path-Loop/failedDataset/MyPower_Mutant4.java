public class MyPower_Mutant4 {
    public static int power(int x, int n) {
        int res = 1;
        for(int i = 0; i <= n; i++){
	        res = res * x;
        }
        return res;
    }
}