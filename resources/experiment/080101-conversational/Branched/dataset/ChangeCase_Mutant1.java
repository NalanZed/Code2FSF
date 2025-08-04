public class ChangeCase_Mutant1 {

    public static char changeCase(char c) {
        char result = ' ';
        if (c < 'a') {
            result = c;
        } else if (c >= 'z') {
            result = (char) (c - 'a' + 'A');
        } else {
            result = 'c';
        }
        return result;
    }
}