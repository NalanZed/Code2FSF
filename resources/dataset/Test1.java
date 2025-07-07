public class Test1{
    public static int test(int n){
        int ans = 0;
        if(n > 0){
            int x = n + 2;
            if(x > 100){
                ans = 1; //path 1
            }
            else{
                ans = 1;//path 2
            }
        }else{
            ans = 2;//path 3
        }
        return ans;
    }
}