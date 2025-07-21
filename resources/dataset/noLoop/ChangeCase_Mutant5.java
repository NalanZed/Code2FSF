public class ChangeCase_Mutant5 {

    public static char changeCase(char c) {
        char result = ' ';
        if (c > 'z') {
            result = c;
        } else if (c >= 'a') {
            result = (char) ('a' - 'A' + c);
        } else if (c > 'Z') {
            result = c;
        } else if (c >= 'A') {
            result = (char) (c - 'A' + 'a');
        } else {
            result = c;
        }
        return result;
    }
}