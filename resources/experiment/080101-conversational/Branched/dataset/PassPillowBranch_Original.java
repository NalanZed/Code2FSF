public class PassPillowBranch_Original {

    public static int passPillow(int n, int time) {
        if(n - 1 < 0){
            throw new IllegalArgumentException("n is less than 1");
        }
        time = time % ((n - 1) * 2);
        if (time < n) {
            return time + 1;
        }
        return n * 2 - time - 1;
    }
}