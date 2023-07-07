import java.util.List;

class DataFlow {

    int a = 0;

    public void parameterFlow(int i, String s) {
        int x = i;
        String y = s;
    }

    public void exitStores() {
        int a = 1;
        int b = 2;
        int c = 3;

        try {
            System.out.println("try");
            int d = a;
        } catch (Exception ex) {
            System.out.println("catch");
            int e = b;
        } finally {
            System.out.println("finally");
            int f = c;
        }
        int z = a + b + c;
    }

    public void override() {
        int a = 1;
        int b = a;
        a = 2;
        int c = a;
    }

    public void compound(int a, int c) {
        c = c + a;
        System.out.println(a);
        System.out.println(c);

        c += a;
        System.out.println(a);
        System.out.println(c);

        c++;
        System.out.println(c);
    }

    public void fieldsHaveNoFlow() {
        int b = this.a;
        int c = a;
        int d = b + c;
    }

    void nameConflict() {
        int b = a;
        int a = 0;

        int c = a;
        int d = this.a;
        int e = a;
        int f = this.a;
    }

    void join() {
        int a = 0;

        if (a == 0) {
            int b = a;
        } else {
            int c = a;
        }

        int d = a;
    }

    void loop() {
        for (int j = 0; j < 10; j++) {
            System.out.println(j);
        }
    }

    int returnFlow() {
        int a = 0;
        return a;
    }
}
