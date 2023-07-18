import java.util.List;

public class NameFlow {

    public int nameFlow(int x) {
        int sum = x, fst = 20;
        int snd = fst;

        sum = add(fst, snd);
        sum = add(sum, 40);
        return sum;
    }

    public int add(int x, int y) {
        return x + y;
    }

    void loopy(int x) {
        int a = 0;
        int b = 100;
        for (int i = 0; i < x; x++) {
            a += x;
            b -= x;
        }
    }

    int bar(int w, int z) {
        int omega = w + z;
        omega = omega + 1;
        return omega;
    }
}
