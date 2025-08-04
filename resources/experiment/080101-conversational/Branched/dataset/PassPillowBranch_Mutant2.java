public class PassPillowBranch_Mutant2 {
    public static int passPillow(int n, int time) {
        time = time % ((n - 1) * 2);
        if (time <= n) { // changed less than to less than or equal to
            return time + 1;
        }
        return n * 2 - time - 1;

}