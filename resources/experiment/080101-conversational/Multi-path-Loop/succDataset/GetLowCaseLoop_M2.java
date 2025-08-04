public class GetLowCaseLoop {
    public static char getLowCase(char c) {
        if(c < 'A' || c > 'F') {
            return 0;
        }
        char t = 'f';
        while(t >= 'a' && t != c - 'A' + 'a') {
            t--;
        }
        return t;
    }
}