public class Return100_Mutant2 {
    public static int return100 () {
        int res = 0;
        int i = 0;
        for(i = 0; i <= 100; i++) { // CHANGED: condition changed to <=
            res = res + 1;
        }
        return res;
    }
}