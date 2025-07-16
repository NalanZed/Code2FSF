public class AddLoopFor {
    public static int addLoop(int x, int y) {
        int sum = y;
        if (x > 0) {
            for(int n = x; n > 0; n--) {
                sum = sum + 1;
            }
        } else {
            for(int n = -x; n > 0; n --) {
                sum = sum - 1;
            }
        }
        return sum;
    }
}
