public class XOR_Mutant4 {
    public static boolean xor(boolean b1, boolean b2) {
        if(b1) {
            if(b2) {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            if(b2) {
                return true;
            }
            else {
                return false;
            }
        }
    }
}