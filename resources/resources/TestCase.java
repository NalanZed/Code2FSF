
            public class TestCase{
                public static int Abs(int num){
                    if(num < 0){
                        System.out.println("Evaluating if condition: num < 0 is evaluated as: " + (num < 0));
                        return -num;
                    }
                    else{
                        return num;
                    }
                }
            
                public static void main(String[] args){
                    int num = -3;
                    int result = TestCase.Abs(num);
                    System.out.println("result = Abs.Abs(num), current value of result: " + result);
                }
            }
            