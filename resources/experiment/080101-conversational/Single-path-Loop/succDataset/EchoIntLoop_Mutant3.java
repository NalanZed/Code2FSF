public class EchoIntLoop_Mutant3 {
    public static int echo(int x) {
        int res = 0;
        for(int i = 1; i < x; i++) {
            res = res + 1;
        }
        return res;
    }
}