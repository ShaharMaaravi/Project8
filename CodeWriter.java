package Project8;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import Project8.Parser.CommandType;


public class CodeWriter {
    
    private BufferedWriter writer;
    private String fileName;
    private int labelCounter = 0;
    private int callCounter = 0;         
    private String currentFunction = "";


    public CodeWriter(String outputFile) { 
        try{
            this.writer = new BufferedWriter(new FileWriter(outputFile));
        } catch (IOException e) {
            throw new RuntimeException("Error opening output file");
        }

        writeInit(); 
    }

    public void writeInit() {
        // SP = 256
        writeLine("@256");
        writeLine("D=A");
        writeLine("@SP");
        writeLine("M=D");
    
        // call Sys.init 0
        writeCall("Sys.init", 0);
    }    

    public void setFileName(String fileName){ 
        this.fileName = fileName; 
        this.currentFunction = ""; 
    }   

    public void writeArithmetic(String command){
        switch (command) {
            case "add": binaryOp("D+M"); break; 
            case "sub": binaryOp("M-D"); break; 
            case "and": binaryOp("D&M"); break; 
            case "or": binaryOp("D|M"); break; 
                
            case "neg": unaryOp("-M"); break; 
            case "not": unaryOp("!M"); break;
            
            case "eq": writeCom("JEQ"); break;
            case "gt": writeCom("JGT"); break;
            case "lt": writeCom("JLT"); break;
        
            default:
                throw new RuntimeException("Unknown command: " + command); 
        }
    }

    
    private void unaryOp(String h) { 
        writeLine("@SP");
        writeLine("A=M-1");
        writeLine("M=" + h);
    }

    private void binaryOp(String h) { 
        //pop y
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M");

        //pop x
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");

        writeLine("M=" + h);

        writeLine("@SP");
        writeLine("M=M+1");
    }

    private void writeCom(String jump) { 
        String trueLabel = "TRUE_" + labelCounter;
        String endLabel  = "END_" + labelCounter;
        labelCounter++;

        // Pop y to D
        writeLine("@SP"); 
        writeLine("M=M-1");
        writeLine("A=M"); 
        writeLine("D=M");

        // Pop x to M, compute x - y
        writeLine("@SP"); 
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M-D");

        // jump if true
        writeLine("@" + trueLabel);
        writeLine("D;" + jump);

        // else: write 0
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=0");
        writeLine("@" + endLabel);
        writeLine("0;JMP");

        // True case: write -1
        writeLine("(" + trueLabel + ")");
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=-1");

        // End: SP++
        writeLine("(" + endLabel + ")");
        writeLine("@SP");
        writeLine("M=M+1");
    }

    public void writePushPop(CommandType c, String segment, int index){
        
        if (c == CommandType.C_POP){
            writePop(segment, index);
        } else if (c == CommandType.C_PUSH){
            writePush(segment, index);
        } else { 
            throw new RuntimeException("Invalid commandType"); 
        }
    }

    private void writePush(String segment, int index) {

        switch (segment) {
            case "constant":
                writeLine("@" + index);
                writeLine("D=A");
                break;

            case "local":    loadFromSegment("LCL", index); break;
            case "argument": loadFromSegment("ARG", index); break;
            case "this":     loadFromSegment("THIS", index); break;
            case "that":     loadFromSegment("THAT", index); break;

            case "temp":
                writeLine("@" + (5 + index));
                writeLine("D=M");
                break;

            case "pointer":
                writeLine(index == 0 ? "@THIS" : "@THAT");
                writeLine("D=M");
                break;

            case "static":
                writeLine("@" + fileName + "." + index);
                writeLine("D=M");
                break;

            default:
                throw new RuntimeException("Bad segment in push: " + segment);
        }

        // push D
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        writeLine("@SP");
        writeLine("M=M+1");
    }

    private void writePop(String segment, int index) {

        switch (segment) {
            case "local":    storeToSegment("LCL", index); break;
            case "argument": storeToSegment("ARG", index); break;
            case "this":     storeToSegment("THIS", index); break;
            case "that":     storeToSegment("THAT", index); break;

            case "temp":
                popToAddress(5 + index);
                break;

            case "pointer":
                popToSymbol(index == 0 ? "THIS" : "THAT");
                break;

            case "static":
                popToSymbol(fileName + "." + index);
                break;

            default:
                throw new RuntimeException("Bad segment in pop: " + segment);
        }
    }

    private void writeLine(String s) { 
        try { 
            writer.write(s);
            writer.newLine();
        }   catch (IOException e) { 
            throw new RuntimeException("Write error");
        }
    }

    public void writeLabel(String label) {
        writeLine("(" + scopedLabel(label) + ")");
    }
    
    public void writeGoto(String label) {
        writeLine("@" + scopedLabel(label));
        writeLine("0;JMP");
    }
    
    public void writeIf(String label) {
        // pop stack top into D; if D != 0 jump
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M");
        writeLine("@" + scopedLabel(label));
        writeLine("D;JNE");
    }

    private String scopedLabel(String label) {
        if (currentFunction == null || currentFunction.isEmpty()) return label;
        return currentFunction + "$" + label;
    } 

    public void writeFunction(String functionName, int nVars) {
        currentFunction = functionName;
    
        writeLine("(" + functionName + ")");
        for (int i = 0; i < nVars; i++) {
            // push constant 0
            writeLine("@0");
            writeLine("D=A");
            writeLine("@SP");
            writeLine("A=M");
            writeLine("M=D");
            writeLine("@SP");
            writeLine("M=M+1");
        }
    }
    
    public void writeCall(String functionName, int nArgs) {
        String returnLabel = functionName + "$ret." + (callCounter++);
    
        // push return-address
        writeLine("@" + returnLabel);
        writeLine("D=A");
        pushD();
    
        // push LCL, ARG, THIS, THAT
        pushFromSymbol("LCL");
        pushFromSymbol("ARG");
        pushFromSymbol("THIS");
        pushFromSymbol("THAT");
    
        // ARG = SP - 5 - nArgs
        writeLine("@SP");
        writeLine("D=M");
        writeLine("@5");
        writeLine("D=D-A");
        writeLine("@" + nArgs);
        writeLine("D=D-A");
        writeLine("@ARG");
        writeLine("M=D");
    
        // LCL = SP
        writeLine("@SP");
        writeLine("D=M");
        writeLine("@LCL");
        writeLine("M=D");
    
        // goto function
        writeLine("@" + functionName);
        writeLine("0;JMP");
    
        // (returnLabel)
        writeLine("(" + returnLabel + ")");
    }

    public void writeReturn() {
        // FRAME = LCL
        writeLine("@LCL");
        writeLine("D=M");
        writeLine("@R13");
        writeLine("M=D");
    
        // RET = *(FRAME-5)
        writeLine("@5");
        writeLine("A=D-A");
        writeLine("D=M");
        writeLine("@R14");
        writeLine("M=D");
    
        // *ARG = pop()
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M");
        writeLine("@ARG");
        writeLine("A=M");
        writeLine("M=D");
    
        // SP = ARG + 1
        writeLine("@ARG");
        writeLine("D=M+1");
        writeLine("@SP");
        writeLine("M=D");
    
        // THAT = *(FRAME-1)
        restoreFromFrame(1, "THAT");
        // THIS = *(FRAME-2)
        restoreFromFrame(2, "THIS");
        // ARG  = *(FRAME-3)
        restoreFromFrame(3, "ARG");
        // LCL  = *(FRAME-4)
        restoreFromFrame(4, "LCL");
    
        // goto RET
        writeLine("@R14");
        writeLine("A=M");
        writeLine("0;JMP");
    }
    
    private void restoreFromFrame(int offset, String target) {
        writeLine("@R13");     // FRAME
        writeLine("D=M");
        writeLine("@" + offset);
        writeLine("A=D-A");
        writeLine("D=M");
        writeLine("@" + target);
        writeLine("M=D");
    }    


    /** helpers **/
    private void loadFromSegment(String base, int index) {
        writeLine("@" + base);
        writeLine("D=M");
        writeLine("@" + index);
        writeLine("A=D+A");
        writeLine("D=M");
    }

    private void storeToSegment(String base, int index) {
        // Compute address → R15
        writeLine("@" + base);
        writeLine("D=M");
        writeLine("@" + index);
        writeLine("D=D+A");
        writeLine("@R15");
        writeLine("M=D");

        // Pop stack → D
        writeLine("@SP");
        writeLine("M=M-1");
        writeLine("A=M");
        writeLine("D=M");

        // Store at computed address
        writeLine("@R15");
        writeLine("A=M");
        writeLine("M=D");
    }

    private void popToAddress(int address) {
        writeLine("@SP"); writeLine("M=M-1");
        writeLine("A=M"); writeLine("D=M");
        writeLine("@" + address);
        writeLine("M=D");
    }

    private void popToSymbol(String symbol) {
        writeLine("@SP"); writeLine("M=M-1");
        writeLine("A=M"); writeLine("D=M");
        writeLine("@" + symbol);
        writeLine("M=D");
    }

    private void pushD() {
        writeLine("@SP");
        writeLine("A=M");
        writeLine("M=D");
        writeLine("@SP");
        writeLine("M=M+1");
    }
    
    private void pushFromSymbol(String sym) {
        writeLine("@" + sym);
        writeLine("D=M");
        pushD();
    }


    public void close(){
        try { 
            writer.close();
        } catch (IOException e) { 
            throw new RuntimeException("Error closing file"); 
        }
    }
}
