public class Return100_Mutant3 {
    public static int return100 () {
        int res = 0;
        for(int i = 0; i < 100; i++) { // CHANGED: moved declaration of i inside the loop
            res = i; // CHANGED: reassigning i to res instead of incrementing res
        }
        return res;
    }
}