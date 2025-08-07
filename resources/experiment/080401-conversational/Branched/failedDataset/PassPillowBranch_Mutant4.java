public class PassPillowBranch_Mutant4 {
    public static int passPillow(int n, int time) {
        time = time % ((n - 1) * 3); // changed multiplier 2 to 3
        if (time < n) {
            return time + 1;
        }
        return n * 2 - time - 1;
    }
}