public class IsDescending {

    public static boolean isDescending(int[] arr) {
        int n = arr.length;
        if (n < 2) {
            return true;
        }
        for (int i = 0; i < n; i++) {
            if (arr[i] <= arr[i + 1])
                return false;
        }
        return true;
    }
}
