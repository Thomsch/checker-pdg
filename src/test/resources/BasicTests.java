import java.util.List;
import java.util.Optional;

class BasicTests {

    float f = 3;

    void conditional() {
        int x = 0;

        if (x == 0) {
            x = 1;
        } else {
            x = 2;
        }
    }

    int sample(int x, int y, List<String> o) {
        int a = x;
        int b = y;
        List<String> oh = o;

        if (a == 3 || a == 2) {
            if (b < 0) {
                boolean c = true && false;
            }

            a = y;
            y++;
        }

        b = x;
        a = y;
        return a + b;
    }
}
