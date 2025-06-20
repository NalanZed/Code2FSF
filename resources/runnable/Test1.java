public class Test1 {

    public static int test(int n) {
        int ans = 0;
        if (n > 0) {
            System.out.println("Evaluating if condition: (n > 0) is evaluated as: " + (n > 0));
            int x = n + 2;
            if (x > 4) {
                System.out.println("Evaluating if condition: (x > 4) is evaluated as: " + (x > 4));
                ans = 1;
            } else {
                System.out.println("Evaluating if condition: !(x > 4) is evaluated as: " + !(x > 4));
                ans = 1;
            }
        } else {
            System.out.println("Evaluating if condition: !(n > 0) is evaluated as: " + !(n > 0));
            ans = 2;
        }
        return ans;
    }

    public static void main(String[] args) {
        int n = 42;
        int result = Test1.test(n);
    }
}
