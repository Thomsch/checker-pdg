public class Switches {

    void basicSwitch(int x) {
        int a;
        switch (x) {
            case 1:
                a = 1;
                break;
            case 2:
                a = 2;
                break;
            default:
                a = 3;
                break;
        }
    }

    void fallThrough(int x) {
        int a;
        switch (x) {
            case 1:
                a = 1;
            case 2:
                a = 2;
                break;
            default:
                a = 3;
                break;
        }
    }
}
