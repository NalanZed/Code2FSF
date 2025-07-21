public class GCD_Mutant1 {
	public static int gcd(int num1, int num2){
		int result = 1;
		if(0 > num1){
			num1 = -num1
		}
		if(0 > num2){
			num2 = -num2;
		}
		if (num1 == 0 && num2 == 0) {
			return -1;
		}
		if (num1 == 0 || num2 == 0) {
			if(num1 > num2) {
				return num1;
			} else {
				return num2;
			}
		}
		for (int i = 1; i <= num1 && i <= num2; i++) {
            if (num1 % i == 0 && num2 % i == 0) {
        		result = i;
			}
        }
		return result + 1; //Mutant: Return result + 1 instead of result
	}
}