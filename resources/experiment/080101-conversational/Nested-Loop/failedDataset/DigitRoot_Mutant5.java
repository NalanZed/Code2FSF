public class DigitRoot_Mutant5 {
    public static int function(int num) {
        while (num >= 10) {
            int sum = 1;
            while (num > 0) {
                sum = sum + (num % 10);
                num = num / 10;
            }
            num = sum;
        }
        return num;
    }
}