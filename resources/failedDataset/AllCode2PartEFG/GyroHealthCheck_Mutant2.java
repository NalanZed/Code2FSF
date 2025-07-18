public class GyroHealthCheck_Mutant2 {
    public static int gyroHealthCheck(int x, int y, int z) {
        int diffXY = x - y;
        int diffYZ = y - z;
        int diffXZ = x - z;
        if (diffXZ < 0) diffXZ = -diffXZ;
        int alarm = 0;
        if (diffXY > 10 || diffYZ > 10 || diffXZ > 10) {
            alarm = 1;
        }
        return alarm;
    }
}