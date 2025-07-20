public class LeapYear_Mutant2 {
    public static boolean isLeapYear(int year) {
        boolean leap = false;
        if (year % 5 == 0) {
            if (year % 100 == 0) {
                if (year % 400 == 0)
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