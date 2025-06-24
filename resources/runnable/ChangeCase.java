public class ChangeCase {

    public static char changeCase1(char c) {
        char result = ' ';
        System.out.println("result = ' ', current value of result: " + (' '));
        if (c > 'z' && c > 'y') {
            System.out.println("Evaluating if condition: (c > 'z' && c > 'y') is evaluated as: " + (c > 'z' && c > 'y'));
            result = c;
            System.out.println("result = c, current value of result: " + result);
        } else if (c >= 'a') {
            System.out.println("Evaluating if condition: (c >= 'a') is evaluated as: " + (c >= 'a'));
            System.out.println("Evaluating if condition: (c >= 'a') is evaluated as: " + (c >= 'a'));
            result = (char) (c - 'a' + 'A');
            System.out.println("result = (char) (c - 'a' + 'A'), current value of result: " + result);
        } else if (c > 'Z') {
            System.out.println("Evaluating if condition: (c > 'Z') is evaluated as: " + (c > 'Z'));
            System.out.println("Evaluating if condition: (c > 'Z') is evaluated as: " + (c > 'Z'));
            System.out.println("Evaluating if condition: (c > 'Z') is evaluated as: " + (c > 'Z'));
            result = c;
            System.out.println("result = c, current value of result: " + result);
        } else if (c >= 'A') {
            System.out.println("Evaluating if condition: (c >= 'A') is evaluated as: " + (c >= 'A'));
            System.out.println("Evaluating if condition: (c >= 'A') is evaluated as: " + (c >= 'A'));
            System.out.println("Evaluating if condition: (c >= 'A') is evaluated as: " + (c >= 'A'));
            System.out.println("Evaluating if condition: (c >= 'A') is evaluated as: " + (c >= 'A'));
            result = (char) (c - 'A' + 'a');
            System.out.println("result = (char) (c - 'A' + 'a'), current value of result: " + result);
        } else {
            System.out.println("Evaluating if condition: !((c > 'z' && c > 'y') || (c >= 'a') || (c > 'Z') || (c >= 'A')) is evaluated as: " + !((c > 'z' && c > 'y') || (c >= 'a') || (c > 'Z') || (c >= 'A')));
            System.out.println("Evaluating if condition: !((c >= 'a') || (c > 'Z') || (c >= 'A')) is evaluated as: " + !((c >= 'a') || (c > 'Z') || (c >= 'A')));
            System.out.println("Evaluating if condition: !((c > 'Z') || (c >= 'A')) is evaluated as: " + !((c > 'Z') || (c >= 'A')));
            System.out.println("Evaluating if condition: !(c >= 'A') is evaluated as: " + !(c >= 'A'));
            result = c;
            System.out.println("result = c, current value of result: " + result);
        }
        return result;
    }

    public static void main(String[] args) {
        char c = '~';
        char result = ChangeCase.changeCase1(c);
    }
}
