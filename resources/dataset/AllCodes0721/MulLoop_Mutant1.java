public class MulLoop_Mutant1 {
    public static int mulLoop(int a, int b) {
        int res = 1; // changed initialization from 0 to 1
        if (b >= 0) {
            for(int i = 0; i < b; i++) {
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