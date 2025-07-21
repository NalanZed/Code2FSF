public class AddLoop_Mutant1 {
    public static int AddLoop(int x, int y) {
        int sum = x;
        if (y > 0) {
            int n = y;
            while (n > 0) {
                sum = sum + 1;
                n = n - 2;
            }
        } else {
            int n = -y;
            while (n > 0) {
                sum = sum - 1;
                n = n - 2;
            }
        }
        return sum;
    }
}