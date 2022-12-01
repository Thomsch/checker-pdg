import java.util.List;
import java.util.Optional;

class BasicTests {

    float f = 3;

    void bar(int w, int z) {
        int omega = w + z;
    }

    void loopy(int x) {
        int a = 0;
        for (int i = 0; i < x; x++) {
            a += x;
        }
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

    void anonymous(Integer x) {
        Integer a = x;
        Optional<Integer> optX = Optional.of(x);
        optX.ifPresent(n -> System.out.println(n));

        Integer b = optX.get();
    }
}
