public class ChangeCase {

public static char changeCase(char c){
char result = ' ';
System.out.println("result = ' ', current value of result: " + result);
if(c > 'z'){
System.out.println("Evaluating if condition: c > 'z' is evaluated as: " + (c > 'z'));
result = c;
System.out.println("result = c, current value of result: " + result);
}else{
if(c >= 'a'){
System.out.println("Evaluating if condition: c >= 'a' is evaluated as: " + (c >= 'a'));
result =(char)(c - 'a' + 'A');
System.out.println("result = (char)(c - 'a' + 'A'), current value of result: " + result);
}else{
if(c > 'Z'){
System.out.println("Evaluating if condition: c > 'Z' is evaluated as: " + (c > 'Z'));
result = c;
System.out.println("result = c, current value of result: " + result);
}else{
if(c >= 'A'){
System.out.println("Evaluating if condition: c >= 'A' is evaluated as: " + (c >= 'A'));
result =(char)(c - 'A' + 'a');
System.out.println("result = (char)(c - 'A' + 'a'), current value of result: " + result);
}else{
result = c;
System.out.println("result = c, current value of result: " + result);
}
}
}
}
return result;
}

public static void main(String[] args){
char c = '~';
System.out.println("c = '~', current value of c: " + c);
char result = ChangeCase.changeCase(c);
System.out.println("result = ChangeCase.changeCase(c), current value of result: " + result);
}
}
