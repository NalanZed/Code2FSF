public class MulLoop_Mutant3 {
    public static int mulLoop(int a, int b) {
        int res = 0;
        if (b >= 0) {
            for(int i = 0; i < b; i++) {
                res = res - a;  // changed the operator from + to -
            }
        } else {
            for(int i = 0; i < -b; i++) {
                res = res + a;  // changed the operator from - to +
            }
        }
        return res;
    }
}