public class GetLowCaseLoop {
    public static char getLowCase(char c) {
        if(c < 'A' || c > 'Z') {
            return 0;
        }
        char t = 'z';
        while(t >= 'a' && t != c - 'A' + 'a') {
            t--;
        }
        return t;
    }
}