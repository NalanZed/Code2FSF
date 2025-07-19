public class LeapYear_Mutant3 {
    public static boolean isLeapYear(int year) {
        boolean leap = false;
        if (year % 4 == 0) {
            if (year % 100 == 0) {
                if (year % 400 == 0)
                    leap = false;
                else
                    leap = true;
            } else
                leap = true;
        } else
            leap = false;
        return leap;
    }
}