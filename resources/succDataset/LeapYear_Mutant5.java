public class LeapYear_Mutant5 {
    public static boolean isLeapYear(int year) {
        boolean leap = false;
        if (year % 4 == 0) {
            if (year % 100 == 0) {
                if (year % 500 == 0)
                    leap = true;
                else
                    leap = false;
            } else
                leap = true;
        } else
            leap = false;
        return leap;
    }
}