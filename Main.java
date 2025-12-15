package Project8;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Project8.Main <inputFile.vm | inputFolder>");
            return;
        }

        File source = new File(args[0]);
        if (!source.exists()) {
            System.err.println("Source not found: " + source.getPath());
            return;
        }

        String outputPath;
        if (source.isDirectory()) {
            // folderName/folderName.asm
            outputPath = new File(source, source.getName() + ".asm").getPath();
        } else {
            // file.vm to file.asm
            if (!source.getName().endsWith(".vm")) {
                System.err.println("Input file must have .vm extension");
                return;
            }
            outputPath = source.getPath().replaceAll("\\.vm$", ".asm");
        }

        // Create ONE CodeWriter for the entire output file
        CodeWriter codeWriter = new CodeWriter(outputPath);

        if (source.isDirectory()) {
            codeWriter.writeInit();
            translateDirectory(source, codeWriter);
        } else {
            translateSingleFile(source, codeWriter);
        }

        codeWriter.close();
    }

    private static void translateDirectory(File dir, CodeWriter codeWriter) {
        File[] vmFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.endsWith(".vm");
            }
        });

        if (vmFiles == null || vmFiles.length == 0) {
            System.err.println("No .vm files found in directory: " + dir.getPath());
            return;
        }

        // Optional but helpful: deterministic order
        Arrays.sort(vmFiles, (a, b) -> a.getName().compareTo(b.getName()));

        // Common convention: translate Sys.vm first if it exists
        File sys = null;
        for (File f : vmFiles) {
            if (f.getName().equals("Sys.vm")) {
                sys = f;
                break;
            }
        }
        if (sys != null) {
            translateSingleFile(sys, codeWriter);
        }
        for (File f : vmFiles) {
            if (sys != null && f.equals(sys)) continue;
            translateSingleFile(f, codeWriter);
        }
    }

    private static void translateSingleFile(File vmFile, CodeWriter codeWriter) {
        Parser parser = new Parser(vmFile.getPath());

        // Set fileName for static variables: Xxx.i
        String baseName = vmFile.getName().replaceFirst("\\.vm$", "");
        codeWriter.setFileName(baseName);

        while (parser.hasMoreLines()) {
            parser.advance();
            Parser.CommandType type = parser.commandType();

            switch (type) {
                case C_ARITHMETIC:
                    codeWriter.writeArithmetic(parser.arg1());
                    break;

                case C_PUSH:
                case C_POP:
                    codeWriter.writePushPop(type, parser.arg1(), parser.arg2());
                    break;

                case C_LABEL:
                    codeWriter.writeLabel(parser.arg1());
                    break;

                case C_GOTO:
                    codeWriter.writeGoto(parser.arg1());
                    break;

                case C_IF:
                    codeWriter.writeIf(parser.arg1());
                    break;

                case C_FUNCTION:
                    codeWriter.writeFunction(parser.arg1(), parser.arg2());
                    break;

                case C_CALL:
                    codeWriter.writeCall(parser.arg1(), parser.arg2());
                    break;

                case C_RETURN:
                    codeWriter.writeReturn();
                    break;

                default:
                    throw new RuntimeException("Unhandled command type: " + type);
            }
        }
    }
}
