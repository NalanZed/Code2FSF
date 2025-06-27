public class Example {

    public static int Abs(int num) {
        if (num < 0) {
            System.out.println("Evaluating if condition: (num < 0) is evaluated as: " + (num < 0));
            int ans = -num;
            System.out.println("ans = -num, current value of ans: " + (-num));
            return ans;
        } else {
            System.out.println("Evaluating if condition: !(num < 0) is evaluated as: " + !(num < 0));
            int ans = num;
            System.out.println("ans = num, current value of ans: " + (num));
            return ans;
        }
    }

    public static void main(String[] args) {
        int num = 428;
        int result = Example.Abs(num);
    }
}
