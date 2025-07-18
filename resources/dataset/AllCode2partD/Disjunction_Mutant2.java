public class Disjunction_Mutant2 {
    public static boolean disjunctOf(boolean b1, boolean b2) {
        if(b1 == true){
            if(b2 == true)
                return true;
        }
        return false;
    }
}