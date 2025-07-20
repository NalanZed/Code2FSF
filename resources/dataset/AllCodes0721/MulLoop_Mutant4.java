public class MulLoop_Mutant4 {
    public static int mulLoop(int a, int b) {
        int res = 0;
        if (b >= 0) {
            for(int i = 0; i < b; i=i+2) { // increased the counter by 2
                res = res + a;
            }
        } else {
            for(int i = 0; i < -b; i=i+2) { // increased the counter by 2
                res = res - a;
            }
        }
        return res;
    }
}