public class Example {
	
	public static int Abs(int num) {
		if (num < 0) {
			int ans = -num;
			return ans;
		}
		else{
			int ans = num;
			return ans;
		}
	}

	public static void main(String[] args) {
		int a = Abs(10);
		System.out.println("Abs(10) = " + a);
	}
}
