package jasmin;

import jas.jasError;

import java.io.*;

public class CompileJASM {

    public static final byte[] assemble(String filename, InputStream fs) throws IOException, JASMParsingFailed, jasError {
        ClassFile classFile = new ClassFile();

        try (InputStreamReader ir = new InputStreamReader(fs)) {
            try (BufferedReader inp = new BufferedReader(ir)) {
                classFile.readJasmin(inp, filename, true /* FIXME maybe? */);
            } catch (Exception e) {
                throw new JASMParsingFailed(e);
            }
        }

        if (classFile.errorCount() > 0) {
            throw new JASMParsingFailed(filename + ": Found " + classFile.errorCount() + " errors");
        }

        String[] class_path = ScannerUtils.splitClassField(classFile.getClassName());
        String class_name = class_path[1];

        try (var outp = new ByteArrayOutputStream()) {
            classFile.write(outp);
            return outp.toByteArray();
        } finally {
            System.out.println("Generated: " + class_name);

            if (classFile.errorCount() > 0) {
                System.err.println(filename + ": Found " + classFile.errorCount() + " errors");
            }
        }
    }
}
