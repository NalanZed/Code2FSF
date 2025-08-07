public class XOR_Mutant5 {
    public static boolean xor(boolean b1, boolean b2) {
        if(b1 == true) {
            if(b2 == true) {
                return false;
            }
            else {
                return b1;
            }
        }
        else {
            if(b2 == true) {
                return b1;
            }
            else {
                return false;
            }
        }
    }
}