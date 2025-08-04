public class PassPillowBranch_Mutant3 {
    public static int passPillow(int n, int time) {
        time = time % ((n - 1) * 2);
        if (time < n) {
            return time + 1;
        }
        return n * 2 - time - 2;
    }
}