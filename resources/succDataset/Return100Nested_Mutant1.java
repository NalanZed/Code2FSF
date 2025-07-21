public class Return100Nested_Mutant1 {
    public static int return100 () {
        int res = 0;
        for(int i = 0; i < 9; i++) {
            for(int j = 0; j < 10; j++) {
                res = res + 1;
            }
        }
        return res;
    }
}