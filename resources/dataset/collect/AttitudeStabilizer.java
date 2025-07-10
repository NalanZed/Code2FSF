public class AttitudeStabilizer {
    public static void attitudeStabilize(String[] args) {
        int pitch = -5, roll = 8;
        int targetPitch = 0, targetRoll = 0;
        int errorPitch = targetPitch - pitch;
        int errorRoll = targetRoll - roll;
        int absPitch = errorPitch < 0 ? -errorPitch : errorPitch;
        int absRoll = errorRoll < 0 ? -errorRoll : errorRoll;
        int controlPitch = 0, controlRoll = 0;
        if (absPitch > 10) {
            controlPitch = errorPitch * 2;
        } else {
            controlPitch = errorPitch;
        }
        if (absRoll > 10) {
            controlRoll = errorRoll * 2;
        } else {
            controlRoll = errorRoll;
        }
        System.out.println("Pitch Signal: " + controlPitch);
        System.out.println("Roll Signal: " + controlRoll);
    }
}