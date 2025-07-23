public class GetLowCaseLoop {
    public static char getLowCase(char c) {
        if(c < 'F' || c > 'Z') {
            return 0;
        }
        char t = 'f';
        while(t <= 'z' && t != c - 'A' + 'a') {
            t++;
        }
        return t;
    }
}