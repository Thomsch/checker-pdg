public class Nested {
    private interface NestedInterface {
        public void apply();
    }


    public class PublicNest implements NestedInterface {
        public void apply() {
            System.out.println("foo");
        }
    }

    public class PrivateNest implements NestedInterface {
        public void apply() {
            System.out.println("bar");
        }
    }

    private class PrivateRunner implements Runnable {
        public void run() {
            System.out.println("Hello");
        }
    }
}
