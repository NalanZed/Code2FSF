public class LCM {

    public static int lcm(int num1, int num2) {
        if (num1 == 0 || num2 == 0) {
            System.out.println("Evaluating if condition: (num1 == 0 || num2 == 0) is evaluated as: " + (num1 == 0 || num2 == 0));
            return -1;
        } else {
            System.out.println("Evaluating if condition: !(num1 == 0 || num2 == 0) is evaluated as: " + !(num1 == 0 || num2 == 0));
        }
        if (num1 < 0) {
            System.out.println("Evaluating if condition: (num1 < 0) is evaluated as: " + (num1 < 0));
            num1 = -num1;
            System.out.println("num1 = -num1, current value of num1: " + num1);
        } else {
            System.out.println("Evaluating if condition: !(num1 < 0) is evaluated as: " + !(num1 < 0));
        }
        if (num2 < 0) {
            System.out.println("Evaluating if condition: (num2 < 0) is evaluated as: " + (num2 < 0));
            num2 = -num2;
            System.out.println("num2 = -num2, current value of num2: " + num2);
        } else {
            System.out.println("Evaluating if condition: !(num2 < 0) is evaluated as: " + !(num2 < 0));
        }
        int result = (num1 > num2) ? num1 : num2;
        System.out.println("result = (num1 > num2) ? num1 : num2, current value of result: " + ((num1 > num2) ? num1 : num2));
        while (result < Integer.MAX_VALUE) {
            if (result % num1 == 0 && result % num2 == 0) {
                System.out.println("Evaluating if condition: (result % num1 == 0 && result % num2 == 0) is evaluated as: " + (result % num1 == 0 && result % num2 == 0));
                break;
            } else {
                System.out.println("Evaluating if condition: !(result % num1 == 0 && result % num2 == 0) is evaluated as: " + !(result % num1 == 0 && result % num2 == 0));
            }
            result++;
        }
        if (result % num1 == 0 && result % num2 == 0) {
            System.out.println("Evaluating if condition: (result % num1 == 0 && result % num2 == 0) is evaluated as: " + (result % num1 == 0 && result % num2 == 0));
            return result;
        } else {
            System.out.println("Evaluating if condition: !(result % num1 == 0 && result % num2 == 0) is evaluated as: " + !(result % num1 == 0 && result % num2 == 0));
        }
        return -1;
    }

    public static void main(String[] args) {
        int num1 = 0;
        int num2 = -407;
        int result = LCM.lcm(num1, num2);
    }
}
