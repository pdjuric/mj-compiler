package rs.ac.bg.etf.pp1;

import java.io.*;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.ast.Visitor;
import rs.etf.pp1.mj.runtime.Code;
import rs.ac.bg.etf.pp1.error_reporting.Reporter;
import rs.ac.bg.etf.pp1.error_reporting.Status;
import rs.ac.bg.etf.pp1.extended.DumpSymbolTableVisitor;

import rs.ac.bg.etf.pp1.extended.Tab;
import rs.ac.bg.etf.pp1.util.Log4JUtils;


import static rs.ac.bg.etf.pp1.error_reporting.Reporter.reporter;

public class Compiler {

    static {
        DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
        Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
    }

    private static final Logger log = Logger.getLogger(Compiler.class);
    private static boolean parserErrorDetected = false, semanticAnalysisErrorDetected = false;
    private static String outputFileName;

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            log.error("Not enough arguments supplied! Usage: MJParser <source-file> <obj-file> ");
            return;
        }

        if (args.length >= 3 && args[2].equals("-no-output")) {
            Logger.getRootLogger().removeAllAppenders();
            Logger.getRootLogger().addAppender(new NullAppender());
            System.setOut(new PrintStream("/dev/null"));
        }

        File sourceCode = new File(args[0]);
        if (!sourceCode.exists()) {
            log.error("Source file [" + sourceCode.getAbsolutePath() + "] not found!");
            return;
        }
        outputFileName = args[1];

        log.info("Compiling source file: " + sourceCode.getAbsolutePath());

        try (Reader br = new BufferedReader(new FileReader(sourceCode));) {
            Yylex lexer = new Yylex(br);
            Program program_ = parse(lexer);
            semanticAnalysis(program_);
            printAST(program_);

            for (Status s: Status.unreportedErrs)
                s.report(null);

            tsdump();

            if (parserErrorDetected || reporter.isErrorDetected()) {
                log.info("Parsing failed");
                return;
            }

            log.info("Parsing successful");

            codeGeneration(program_);
            tsdump();
            writeCodeToFile();
        }
    }

    private static Program parse(Yylex lexer) throws Exception{
        MJParser parser = new MJParser(lexer);
        Program program_ =  (Program) parser.parse().value;
        parserErrorDetected = parser.errorDetected;
        return program_;
    }

    private static void semanticAnalysis(Program program_) {
        Tab.init();
        Reporter.init();
        Visitor semanticAnalyzer = new SemanticAnalyzer();
        program_.traverseBottomUp(semanticAnalyzer);
    }

    private static void codeGeneration(Program program_) {
        Visitor codeGenerator = new CodeGenerator();
        program_.traverseBottomUp(codeGenerator);
    }

    public static void printAST(Program program_) {
        log.info("\n" + program_.toString(""));
    }

    public static void writeCodeToFile() {
        File objFile = new File(outputFileName);
        if (objFile.exists()) objFile.delete();

        try (FileOutputStream objFileStream = new FileOutputStream(objFile)) {
            Code.write(objFileStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void tsdump() {
        Tab.dump(new DumpSymbolTableVisitor());
    }

}
