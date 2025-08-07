public class PassPillowBranch_Mutant5 {
    public static int passPillow(int n, int time) {
        time = time % ((n - 1) * 2);
        if (time < n) {
            return time + 1;
        }
        return n * 3 - time - 1; // changed multiplication factor from 2 to 3
    }
}