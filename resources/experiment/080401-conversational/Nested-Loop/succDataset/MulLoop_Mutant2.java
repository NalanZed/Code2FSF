public class MulLoop_Mutant2 {
    public static int mulLoop(int a, int b) {
        int res = 0;
        if (b > 0) { // changed >= to >
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