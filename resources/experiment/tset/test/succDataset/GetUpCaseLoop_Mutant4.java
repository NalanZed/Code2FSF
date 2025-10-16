public class GetUpCaseLoop {
    public static char getUpCase(char c) {
        if(c < 'a' || c > 'z') {
            return 0;
        }
        char t = 'A';
        while(t <= 'Z' && t != c - 'a' + 'A') {
            t++;
        }
        return t;
    }
}