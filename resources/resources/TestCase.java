public class TestCase {

    public static int altitudeController(int currentHeight, int targetHeight) {
        System.out.println("Function input int parameter targetHeight = " + (targetHeight));
        System.out.println("Function input int parameter currentHeight = " + (currentHeight));
        int error = targetHeight - currentHeight;
        System.out.println("error = (targetHeight - currentHeight), current value of error: " + (targetHeight - currentHeight));
        int absError = 0;
        System.out.println("absError = (0), current value of absError: " + (0));
        absError = error < 0 ? -error : error;
        System.out.println("Under condition absError = (error), condition is : " + !(error < 0));
        System.out.println("Under condition absError = (-error), condition is : " + (error < 0));
        int controlSignal = 0;
        System.out.println("controlSignal = (0), current value of controlSignal: " + (0));
        if (absError >= 30) {
            System.out.println("Evaluating if condition: (absError >= 30) is evaluated as: " + (absError >= 30));
            //Changed > to >=
            controlSignal = error > 0 ? 5 : -5;
            System.out.println("Under condition controlSignal = (-5), condition is : " + !(error > 0));
            System.out.println("Under condition controlSignal = (5), condition is : " + (error > 0));
        } else if (absError > 20) {
            System.out.println("Evaluating if condition: (absError > 20) is evaluated as: " + (absError > 20));
            controlSignal = error > 0 ? 3 : -3;
            System.out.println("Under condition controlSignal = (-3), condition is : " + !(error > 0));
            System.out.println("Under condition controlSignal = (3), condition is : " + (error > 0));
        } else if (absError > 10) {
            System.out.println("Evaluating if condition: (absError > 10) is evaluated as: " + (absError > 10));
            controlSignal = error > 0 ? 2 : -2;
            System.out.println("Under condition controlSignal = (-2), condition is : " + !(error > 0));
            System.out.println("Under condition controlSignal = (2), condition is : " + (error > 0));
        } else {
            System.out.println("Evaluating if condition: !((absError >= 30) || (absError > 20) || (absError > 10)) is evaluated as: " + !((absError >= 30) || (absError > 20) || (absError > 10)));
            controlSignal = error;
            System.out.println("controlSignal = (error), current value of controlSignal: " + (controlSignal));
        }
        System.out.println("return_value = controlSignal , current value of return_value : " + (controlSignal));
        return controlSignal;
    }

    public static void main(String[] args) {
        int currentHeight = -30;
        int targetHeight = 1;
        int result = AltitudeController_Mutant4.altitudeController(currentHeight, targetHeight);
    }
}