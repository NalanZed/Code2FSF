public class MulLoop {

	public static int mulLoop(int a, int b) {
        int res = 0;
        if (b >= 0) {
            int i = 0;
            for(i = 0; i < b; i++) {
                res = res + a;
            }
        } else {
            int i = 0;
            for(i = 0; i < -b; i++) {
                res = res - a;
            }
        }
        return res;
    }

}