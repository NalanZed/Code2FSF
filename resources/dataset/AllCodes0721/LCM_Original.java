public class LCM_Original {

    public static int lcm(int num1, int num2)
    {
        if (num1 == 0 || num2 == 0) {
            return -1;
        }
        if (num1 < 0)
            num1 = -num1;
        if (num2 < 0)
            num2 = -num2;

        int result = 0;
        if( num1 > num2 ){
            result = num1;
        }
        else{
            result = num2;
        }

            while (result < Integer.MAX_VALUE)
            {
                if (result % num1 == 0 && result % num2 == 0)
                {
                    break;
                }
                result++;
            }

        if (result % num1 == 0 && result % num2 == 0) {
            return result;
        }
        return -1;
    }

}