public class Return100_Mutant1 {
    public static int return100 () {
        int res = 0;
        int i = 0;
        for(i = 0; i < 100; i++) {
            res = res + 2; // CHANGED: incremented by 2 instead of 1
        }
        return res;
    }
}