public class AddLoopFor_Mutant2 {
    public static int addLoop(int x, int y) {
        int sum = y;
        if (x > 0) {
            int n = 0;
            for(n = x; n > 0; ) {
                sum = sum + 1;
                n = n - 2;
            }
        } else {
            int n = 0;
            for(n = -x; n > 0; ) {
                sum = sum - 1;
                n = n - 2;
            }
        }
        return sum;
    }
}