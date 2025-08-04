public class XOR_Mutant1 {
    public static boolean xor(boolean b1, boolean b2) {
        if(b1 == true) {
            return !b2;
        }
        else {
            return b2;
        }
    }
}