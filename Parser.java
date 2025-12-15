package Project8;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
    
    public enum CommandType {
        C_ARITHMETIC,
        C_PUSH,
        C_POP, 
        C_LABEL,
        C_GOTO,
        C_IF,
        C_FUNCTION,
        C_RETURN,
        C_CALL
    }

    private BufferedReader reader;
    private String currentLine;
    private String nextLine;  // lookahead buffer
    
    //constructor
    public Parser(String inputFilePath) {
        try {
            this.reader = new BufferedReader(new FileReader(inputFilePath));
            this.currentLine = null;
            this.nextLine = null;
        } catch (IOException e) {
            throw new RuntimeException("Error opening file: " + inputFilePath, e);
        }
    }

    public boolean hasMoreLines() {
        if (nextLine != null) return true;  
        try {
            String line;
            while ((line = reader.readLine()) != null) {

                // remove inline comments
                int commentIndex = line.indexOf("//");
                if (commentIndex != -1) {
                    line = line.substring(0, commentIndex);
                }

                // trim whitespace
                line = line.trim();

                // skip empty lines
                if (!line.isEmpty()) {
                    nextLine = line;     
                    return true;
                }
            }
            return false;   
        } catch (IOException e) {
            return false;
        }
    }

    public void advance() {
        if (!hasMoreLines()) {
            throw new IllegalStateException("No more lines to advance to.");
        }
        currentLine = nextLine;
        nextLine = null;  // consume it
    }

    public CommandType commandType() {
        // 1. Get the first word of the current line (the command)
        // Assuming 'currentLine' is the cleaned line you processed earlier
        String command = currentLine.split("\\s+")[0]; 
    
        switch (command) {
            // --- Arithmetic / Logical Commands ---
            // These all share the same CommandType
            case "add":
            case "sub":
            case "neg":
            case "eq":
            case "gt":
            case "lt":
            case "and":
            case "or":
            case "not":
                return CommandType.C_ARITHMETIC;
    
            // --- Memory Access Commands ---
            case "push":
                return CommandType.C_PUSH;
            case "pop":
                return CommandType.C_POP;

            // branching
            case "label":   return CommandType.C_LABEL;
            case "goto":    return CommandType.C_GOTO;
            case "if-goto": return CommandType.C_IF;

            // functions
            case "function": return CommandType.C_FUNCTION;
            case "call":     return CommandType.C_CALL;
            case "return":   return CommandType.C_RETURN;
    
            default:
                throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    public String arg1() {
        CommandType type = commandType();
        String[] parts = currentLine.split("\\s+");
    
        if (type == CommandType.C_RETURN) {
            throw new IllegalStateException("arg1 should not be called for C_RETURN");
        }
        if (type == CommandType.C_ARITHMETIC) {
            return parts[0];
        }
        return parts[1];
    }
    
    
    public int arg2() {
        CommandType type = commandType();
        if (!(type == CommandType.C_PUSH ||
              type == CommandType.C_POP ||
              type == CommandType.C_FUNCTION ||
              type == CommandType.C_CALL)) {
            throw new IllegalStateException("arg2() is not valid for command type: " + type);
        }
    
        String[] parts = currentLine.split("\\s+");
        return Integer.parseInt(parts[2]);
    }
    
}
