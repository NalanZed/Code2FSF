public class GetUpCaseLoop {
    public static char getUpCase(char c) {
        if(c < 'f' || c > 'z') {
            return 0;
        }
        char t = 'F';
        while(t <= 'Z' && t != c - 'a' + 'A') {
            t++;
        }
        return t;
    }
}