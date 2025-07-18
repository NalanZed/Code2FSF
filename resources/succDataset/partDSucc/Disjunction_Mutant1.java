public class Disjunction_Mutant1 {
    public static boolean disjunctOf(boolean b1, boolean b2) {
        if(b1 == false)
            return true;
        if(b2 == true)
            return true;
        return false;
    }
}