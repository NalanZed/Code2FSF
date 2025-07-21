public class Return100Nested_Mutant5 {
    public static int return100 () {
        int res = 0;
        for(int j = 0; j < 10; j++) {
            for(int i = 0; i < 10; i++) {
                res = res + 1;
            }
        }
        return res;
    }
}