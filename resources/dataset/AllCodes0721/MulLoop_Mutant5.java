public class MulLoop_Mutant5 {
    public static int mulLoop(int a, int b) {
        int res = 0;
        if (b >= 0) {
            for(int i = b; i > 0; i--) { // changed the loop to decrement from b to 0
                res = res + a;
            }
        } else {
            for(int i = 0; i < -b; i++) {
                res = res - a;
            }
        }
        return res;
    }
}