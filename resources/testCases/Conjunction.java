public class Conjunction {

    public static boolean conjunctOf(boolean b1, boolean b2) {
        if (b1 == false) {
            System.out.println("Evaluating if condition: (b1 == false) is evaluated as: " + (b1 == false));
            System.out.println("return_value = false , current value of return_value : " + false);
            return false;
        } else {
            System.out.println("Evaluating if condition: !(b1 == false) is evaluated as: " + !(b1 == false));
        }
        if (b2 == false) {
            System.out.println("Evaluating if condition: (b2 == false) is evaluated as: " + (b2 == false));
            System.out.println("return_value = false , current value of return_value : " + false);
            return false;
        } else {
            System.out.println("Evaluating if condition: !(b2 == false) is evaluated as: " + !(b2 == false));
        }
        System.out.println("return_value = true , current value of return_value : " + true);
        return true;
    }

    public static void main(String[] args) {
        boolean b1 = false;
        boolean b2 = false;
        boolean result = Conjunction.conjunctOf(b1, b2);
    }
}