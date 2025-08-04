public class MyPower_Mutant1 {
    public static int power(int x, int n) {
        int res = 0;
        for(int i = 0; i < n; i++){
            res = res * x;
        }
        return res;
    }
}