public class Example {

public static int Abs(int num){
if(num < 0){
System.out.println("Evaluating if condition: num < 0 is evaluated as: " + (num < 0));
int ans = -num;
System.out.println("ans = -num, current value of ans: " + ans);
return ans;
}else{
int ans = num;
System.out.println("ans = num, current value of ans: " + ans);
return ans;
}
}

public static void main(String[] args){
int num = -115;
System.out.println("num = -115, current value of num: " + num);
int result = Example.Abs(num);
System.out.println("result = Example.Abs(num), current value of result: " + result);
}
}
