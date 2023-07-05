class DataFlow {

    int a = 0;

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
}
