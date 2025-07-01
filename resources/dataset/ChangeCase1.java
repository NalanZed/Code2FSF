public class ChangeCase {

  public static char changeCase1(char c) {
    char result = ' ';    
    if (c > 'z' && c > 'y') {
      result = c;
    } else if (c >= 'a') {
      result =  (c - 'a' + 'A');
    } else if (c > 'Z') {
      result =  c;
    } else if (c >= 'A') {
      result =  (c - 'A' + 'a');
    } else {
      result = c;
    }
    return result;
  }

}
