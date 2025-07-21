public class PassPillowBranch_Mutant1 {
    public static int passPillow(int n, int time) {
        time = time % (n + 1) * 2; // changed subtraction to addition
        if (time < n) {
            return time + 1;
        }
        return n * 2 - time - 1;
    }
}