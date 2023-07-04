class LocalMethodCalls {

    void local(){
        int a = bar(1,2);
        int b = bar(3);
    }

    void chained() {
        int a = getThis().bar(4,5);
    }

    void nested() {
        int a = bar(bar(6,7),bar(8));
    }

    void localAndNotLocal() {
        int a = bar(5);
        int b = Math.abs(a);
    }

    int bar(int w,int z){
        int omega=w+z;
        omega=omega+1;
        omega++;
        return omega;
    }

    int bar(int y){
        return y+1;
    }

    LocalMethodCalls getThis() {
        return this;
    }



}
