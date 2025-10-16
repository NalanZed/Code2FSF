public class WaterFee_Mutant2 {
    public static int calcWaterFee(int tons) {
        int n = tons;
        int fee = 0;
        while (n > 0) {
            if (n > 10) {
                fee += 5;
            } else if (n > 3) {
                fee += 4;
            } else {
                fee += 3;
            }
            n--;
        }
        return fee;
    }
}