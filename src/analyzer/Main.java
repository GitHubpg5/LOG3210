package analyzer;

import analyzer.ast.*;

import java.io.*;

/**
 * Created: 17-08-02
 * Author: Nicolas Cloutier
 * Last Changed: 23-01-16
 * Author: Raphael Tremblay
 *
 * Description: This class contains one of the two entry points of the program.
 * (The other one is TestRunner.main()). But Main.Run() is used by both entries.
 */

public class Main {

    // It is the entry point of the programs
    // The arguments are:
    // arg[0] (Required): The path of the input file to parse
    // arg[1] (Optional): The path of the output file, will print to System.out if missing.
    public static void main(String[] args) {
        if(args.length <= 0) {
            System.err.println("args[0] is missing! (The file to read)");
            return;
        }

        InputStream file;
        try {
            file = new java.io.FileInputStream(args[0]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        // For this test we are using the print visitor, but you can change this
        // to whatever you want.
        PrintStream pw = new PrintStream(System.out);
        try {
            Run(file, pw);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    // This is the main function of the program, it is used by main and it is used
    // by the test cases.
    public static void Run(InputStream input, PrintStream output) throws ParseException {
        PrintStream stdout = new PrintStream(System.out);
        System.setOut(output);

        // This line ask the parser built from the jjt file to read & parse the input file
        ASTProgram root = Parser.ParseTree(input);
        root.dump("  ");

        System.setOut(stdout);

        // If the visitor has printed in the output, make sure everything is flushed
        output.flush();
    }
}
