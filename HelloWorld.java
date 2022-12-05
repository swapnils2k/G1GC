import java.util.*;
public class HelloWorld {

    public static void main (String[] args) {
        int counter = 0;

        while(counter < 1) {
            Person a = new Person(1);
            System.out.println(a.getX());
            counter++;
            System.out.println(counter);
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

