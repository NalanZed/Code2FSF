public class Abs {

    public static int Abs(int num) {
        if (num < 0) {
            System.out.println("Evaluating if condition: (num < 0) is evaluated as: " + (num < 0));
            return -num;
        } else {
            System.out.println("Evaluating if condition: !(num < 0) is evaluated as: " + !(num < 0));
            return num;
        }
    }

    public static void main(String[] args) {
        int num = 143;
        int result = Abs.Abs(num);
    }
}
