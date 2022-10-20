import org.checkerframework.checker.codechanges.qual.*;

// Test basic subtyping relationships for the Dataflow Checker.
class SubtypeTest {
    void allSubtypingRelationships(@DataflowUnknown int x, @DataflowBottom int y) {
        @DataflowUnknown int a = x;
        @DataflowUnknown int b = y;

        b = x;
        // :: error: assignment
        @DataflowBottom int c = x; // expected error on this line
        @DataflowBottom int d = y;
    }
}
