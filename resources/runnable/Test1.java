public class Test1 {

    public static int test(int n) {
        int ans = 0;
        System.out.println("ans = 0, current value of ans: " + (0));
        //T: n > 0  T && !(n+2>100)
        if (n > 0) {
            System.out.println("Evaluating if condition: (n > 0) is evaluated as: " + (n > 0));
            int x = n + 2;
            System.out.println("x = n + 2, current value of x: " + (n + 2));
            if (x > 100) {
                System.out.println("Evaluating if condition: (x > 100) is evaluated as: " + (x > 100));
                //n+2>100
                ans = 1;
                System.out.println("ans = 1, current value of ans: " + ans);
            } else {
                System.out.println("Evaluating if condition: !(x > 100) is evaluated as: " + !(x > 100));
                //0<n+2<100
                ans = 1;
                System.out.println("ans = 1, current value of ans: " + ans);
            }
        } else {
            System.out.println("Evaluating if condition: !(n > 0) is evaluated as: " + !(n > 0));
            ans = 2;
            System.out.println("ans = 2, current value of ans: " + ans);
        }
        return ans;
    }

    public static void main(String[] args) {
        int n = 137;
        int result = Test1.test(n);
    }
}
