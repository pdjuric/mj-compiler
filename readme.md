# MJ Compiler

This project is a compiler for MicroJava language, written in Java. It is a part of the course "Principles of Programming Languages" at the School of Electrical Engineering, University of Belgrade.
Project scope statement can be found [here](pp1_2022_2023_jan.pdf), and MicroJava specification [here](microjava_spec.pdf).


## Dependencies & Requirements
The project must be compiled with **Java 8**. It is recommended to use IntelliJ IDEA as an IDE.

All the project dependencies are stored in `lib` folder. They are:
* [AST-CUP](lib/cup_v10k.jar) - parser generator, extension of CUP parser generator with Abstract Syntax Trees support.
* [JFlex](lib/JFlex.jar) - lexer generator.
* [Log4j](lib/log4j-1.2.17.jar) - logging framework.
* [MJ Runtime](lib/mj-runtime.jar) - runtime library for MicroJava language.
* [Symbol Table](lib/symboltable-1-1.jar) - symbol table library.

Add all of the above as external libraries to your project (Project Structure > Libraries).

## Lexer
Lexer is specified in [mjlexer.flex](spec/mjlexer.flex) file.
Run Ant target `generate_lexer` to generate Lexer using JFlex.

## Parser
Parser is specified in [mjparser.cup](spec/mjparser.cup) file. Currently, IntelliJ IDEA does not parse .cup files correctly,
so this file is generated from two files:
- [cup_prefix.txt](cup_gen/cup_prefix.txt) - contains package and import statements and parser code.
- [template.cup](cup_gen/template.cup) - contains grammar rules; every rule is written in a separate row, without `|` separator, and its name is commented out.

[script.sh](cup_gen/script.sh) is a script that generates `mjparser.cup` from these two files. Ant target `generate_cup` runs this script.

After running `generate_cup`, run Ant target `generate_parser` to generate Parser using AST-CUP. For debugging purposes, enable `-dump_states` option in [build.xml](build.xml) file.


## Compiler

To generate compiler, run Ant target `generate_compiler`, and compile MJ code with Ant target `compile`. Input and output are specified in [build.xml](build.xml) file, under `compile` target.

Alternatively, you can run 
```sh
java -Dfile.encoding=UTF-8 -classpath "out:config:lib/*" rs.ac.bg.etf.pp1.Compiler [source-file] [obj-file] 
``` 
to compile the MJ code. 


Alternatively, run the Compiler class through IDE Run Configuration. In IntelliJ IDEA, you may encounter the following error:
```
Exception in thread "main" java.lang.ExceptionInInitializerError
Caused by: java.lang.NullPointerException
	at org.apache.log4j.xml.DOMConfigurator$2.toString(DOMConfigurator.java:775)
	at org.apache.log4j.xml.DOMConfigurator.doConfigure(DOMConfigurator.java:878)
	at org.apache.log4j.xml.DOMConfigurator.doConfigure(DOMConfigurator.java:778)
	at org.apache.log4j.xml.DOMConfigurator.configure(DOMConfigurator.java:906)
	at rs.ac.bg.etf.pp1.Compiler.<clinit>(Compiler.java:25)
```
To fix this, set `config` folder as resources root in your project.
In Settings > Compiler > Resource Patterns, add `config/**` to the list of patterns.

## MJ Runtime
To execute compiled MJ code, run Ant target `execute`. Input is specified in [build.xml](build.xml) file, under `execute` target. 

To debug the code, add `-debug` option to the `execute` Ant target. This will print each executed instruction, and expression stack after each instruction.

To view the disassembly of the compiled code, run Ant target `disassemble`. Input is specified in [build.xml](build.xml) file, under `disassemble` target.
