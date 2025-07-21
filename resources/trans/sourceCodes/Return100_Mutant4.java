public class Return100_Mutant4 {
    public static int return100 () {
        int res = 1; // CHANGED: initialized res to 1 instead of 0
        int i = 0;
        for(i = 0; i < 100; i++) {
            res = res + 1;
        }
        return res;
    }
}