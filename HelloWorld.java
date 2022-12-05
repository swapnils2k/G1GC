import java.util.*;
public class HelloWorld {

    public static void main (String[] args) {
        // System.out.println("Hello World");
        int counter = 0;
        while(true) {
            Person a = new Person(1);
            // System.out.println(a.getX());
            counter++;
            // System.out.println(counter);
        }
    }


    public static class Person {
        int x;

        Person(int x) {
            this.x = x;
        }
        
        public static Person getPersonObject(int x) {
            return new Person(x);
        }
        

        public int getX() {
            return this.x;
        }

    }
}

