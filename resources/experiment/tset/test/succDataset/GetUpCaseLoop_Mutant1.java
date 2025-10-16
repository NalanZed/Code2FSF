public class GetUpCaseLoop {
    public static char getUpCase(char c) {
        if(c < 'a' || c > 'f') {
            return 0;
        }
        char t = 'F';
        while(t >= 'A' && t != c - 'a' + 'A') {
            t--;
        }
        return t;
    }
}