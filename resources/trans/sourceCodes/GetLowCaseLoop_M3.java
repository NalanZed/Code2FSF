public class GetLowCaseLoop {
    public static char getLowCase(char c) {
        if(c < 'A' || c > 'Z') {
            return 0;
        }
        char t = 'a';
        while(t <= 'z' && t != c - 'A' + 'a') {
            t++;
        }
        return t;
    }
}