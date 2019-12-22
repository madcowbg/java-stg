package compilation;

import tea.stg2.parser.Bind;

public class Stg2ToJavaCompiler {
    private static final String MAIN_CLASS_NAME = "MainAppClass";
    private final StringBuilder content = new StringBuilder();

    public Stg2ToJavaCompiler(Bind[] graph) {
        // fixme dummy ...
        content.append("public class " + MAIN_CLASS_NAME + " {\n" +
                "\n" +
                "    public static void main(String[] var0) {\n" +
                "        System.out.println(\"Hello World!\");\n" +
                "        int var2 = 5 + 37;\n" +
                "        String var1 = String.valueOf(var2);\n" +
                "        System.out.println(var1);\n" +
                "    }\n" +
                "}\n");
    }

    public String mainClassName() {
        return MAIN_CLASS_NAME;
    }

    public byte[] byteArray() {
        return content.toString().getBytes();
    }
}
