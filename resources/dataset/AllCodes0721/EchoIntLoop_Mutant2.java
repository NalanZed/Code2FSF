public class EchoIntLoop_Mutant2 {
    public static int echo(int x) {
        int res = 1;
        for(int i = 0; i < x; i++) {
            res = res + 1;
        }
        return res;
    }
}