public class AltitudeController_Mutant2 {
    public static int altitudeController(int currentHeight,int targetHeight) {
        int error = targetHeight - currentHeight;
        int absError = 0;
        absError = error < 0 ? -error : error;
        int controlSignal = 1; //Changed 0 to 1
        if (absError > 30) {
            controlSignal = error > 0 ? 5 : -5;
        } else if (absError > 20) {
            controlSignal = error > 0 ? 3 : -3;
        } else if (absError > 10) {
            controlSignal = error > 0 ? 2 : -2;
        } else {
            controlSignal = error;
        }
        return controlSignal;
    }
}