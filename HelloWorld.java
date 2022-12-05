import java.util.*;
public class HelloWorld {

    public static void main (String[] args) {
        // System.out.println("Hello World");
        List<Person> p= new ArrayList<Person>();
        while(true) 
            p.add(new Person(1));
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

