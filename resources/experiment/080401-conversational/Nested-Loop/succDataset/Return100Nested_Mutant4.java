public class Return100Nested_Mutant4 {
    public static int return100 () {
        int res = 1;
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 10; j++) {
                res = res + 1;
            }
        }
        return res;
    }
}