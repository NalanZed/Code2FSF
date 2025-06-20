public class Test1 {

    public static int test(int n) {
        int ans = 0;
        if (n > 0) {
            int x = n + 2;
            if (x > 4) {
                ans = 1;
            } else {
                ans = 1;
            }
        } else {
            ans = 2;
        }
        return ans;
    }

    public static void main(String[] args) {
        int n = 42;
        int result = Test1.test(n);
    }
}
