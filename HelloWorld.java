import java.util.*;
public class HelloWorld {

    public static void main (String[] args) {
	    List list = new ArrayList();
        int counter = 0;
        while(true) {
            list.add(new ArrayList());
            counter++;
            System.out.println(counter);
        }
    }
}
