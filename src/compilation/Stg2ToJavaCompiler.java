package compilation;

import tea.stg2.parser.Bind;

public class Stg2ToJavaCompiler extends SourceBuilder {
    private static final String MAIN_CLASS_NAME = "MainAppClass";

    public Stg2ToJavaCompiler(Bind[] graph) {
        super("//");

        header();

        declareRegisters();

        src("   public static void main(String[] var0) {");
        src("       System.out.println(\"Hello World!\");");
        src("       A = 5; ");
        src("       B = 37; ");
        src("       int var2 = A + B;");
        src("       String var1 = String.valueOf(var2);");
        src("       System.out.println(var1);");
        src("   }");
        footer();
    }

    private void declareRegisters() {
        src("   private static int Sp;");
        src("   private static int A;");
        src("   private static int B;");
    }

    private void header() {
        src("public class " + MAIN_CLASS_NAME + " {");
    }

    private void footer() {
        src("}");
    }

    public String mainClassName() {
        return MAIN_CLASS_NAME;
    }

}
