import java.util.List;

class BasicTests {

    float f = 3;

    void bar(int w, int z) {
        int omega = w + z;
    }

    int sample(int x, int y, List<String> o) {
        int a = x;
        int b = y;
        List<String> oh = o;

        //bar(a, b); // Unsupported.
        // This is a comment
        if (a == 3 || a == 2) {
            if(b < 0) {
                boolean c = true && false;
            }

            a = y;
            y++;
        }

        b = x;
        a = y;
        return a + b;
    }

    void exceptionTest(int x) {
        int a = x;

        try {
            int b = a / x;
        } catch (Exception e) {
            int c = x;
        }
    }
}
