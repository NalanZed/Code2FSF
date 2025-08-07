public class GyroHealthCheck_Mutant1 {
    public static int gyroHealthCheck(int x, int y, int z) {
        int diffXY = x - y;
        if (diffXY > 0) diffXY = -diffXY;
        int diffYZ = y - z;
        if (diffYZ > 0) diffYZ = -diffYZ;
        int diffXZ = x - z;
        if (diffXZ < 0) diffXZ = -diffXZ;
        int alarm = 0;
        if (diffXY > 10 || diffYZ > 10 || diffXZ > 10) {
            alarm = 1;
        }
        return alarm;
    }
}