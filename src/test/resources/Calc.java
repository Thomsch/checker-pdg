import java.util.List;
import java.util.Optional;

/**
 * Toy example from "RefiNym: Using Names to Refine Types" adapted to Java.
 */
class Calc {

    public static void main(String[] args) {
        Calc c = new Calc();
        int sum;
        int fst = 20;
        int snd = fst;
        boolean b = true;
        Thread t = new Thread();

        sum = c.add(fst, snd);
        sum = c.add(sum, 40);
        // int sum2 = sum + fst + 10 + snd + c.add(3,4);
    }

    public int add(int x, int y) {
        return x + y;
    }
}
