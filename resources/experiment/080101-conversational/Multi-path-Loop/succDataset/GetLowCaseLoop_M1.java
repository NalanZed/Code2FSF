public class GetLowCaseLoop {
    public static char getLowCase(char c) {
        if(c < 'F' || c > 'Z') {
            return 0;
        }
        char t = 'z';
        while(t >= 'f' && t != c - 'A' + 'a') {
            t--;
        }
        return t;
    }
}