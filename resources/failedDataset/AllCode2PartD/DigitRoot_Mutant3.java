public class DigitRoot_Mutant3 {
    public static int function(int num) {
        while (num >= 10) {
            int sum = 0;
            while (num > 0) {
                sum = sum - (num % 10);
                num = num / 10;
            }
            num = sum;
        }
        return num;
    }
}