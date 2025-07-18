public class AddLoopFor_Mutant4 {
    public static int addLoop(int x, int y) {
        int sum = y;
        if (x > 0) {
            int n = 0;
            for(n = x; n > 0; ) {
                sum = sum - 1;
                n = n - 1;
            }
        } else {
            int n = 0;
            for(n = -x; n > 0; ) {
                sum = sum + 1;
                n = n - 1;
            }
        }
        return sum;
    }
}