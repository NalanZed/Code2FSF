public class XOR_Mutant3 {
    public static boolean xor(boolean b1, boolean b2) {
        if(b1 == true) {
            if(b2 == true) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            if(b2 == true) {
                return false;
            }
            else {
                return true;
            }
        }
    }
}