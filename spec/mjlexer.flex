package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}

    private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}
%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\n" 	{ }
"\r"    { }
"\f" 	{ }

"program"	{ return new_symbol(sym.PROGRAM, yytext()); }
"break"		{ return new_symbol(sym.BREAK, yytext()); }
"class"		{ return new_symbol(sym.CLASS, yytext()); }
"enum"		{ return new_symbol(sym.ENUM, yytext()); }
"else"		{ return new_symbol(sym.ELSE, yytext()); }
"const"		{ return new_symbol(sym.CONST, yytext()); }
"if"		{ return new_symbol(sym.IF, yytext()); }
"do"		{ return new_symbol(sym.DO, yytext()); }
"while"		{ return new_symbol(sym.WHILE, yytext()); }
"new"		{ return new_symbol(sym.NEW, yytext()); }
"print"		{ return new_symbol(sym.PRINT, yytext()); }
"read"		{ return new_symbol(sym.READ, yytext()); }
"return"	{ return new_symbol(sym.RETURN, yytext()); }
"void"		{ return new_symbol(sym.VOID, yytext()); }
"extends"	{ return new_symbol(sym.EXTENDS, yytext()); }
"continue"	{ return new_symbol(sym.CONTINUE, yytext()); }
"foreach"	{ return new_symbol(sym.FOREACH, yytext()); }



"++"		{ return new_symbol(sym.INC, yytext()); }
"--"		{ return new_symbol(sym.DEC, yytext()); }
"+"		    { return new_symbol(sym.PLUS, yytext()); }
"-"		    { return new_symbol(sym.MINUS, yytext()); }
"*"		    { return new_symbol(sym.ASTERISK, yytext()); }
"/"		    { return new_symbol(sym.SLASH, yytext()); }
"%"		    { return new_symbol(sym.PERCENT, yytext()); }
"=="		{ return new_symbol(sym.EQ, yytext()); }
"!="		{ return new_symbol(sym.NEQ, yytext()); }
">"		    { return new_symbol(sym.GT, yytext()); }
">="		{ return new_symbol(sym.GTE, yytext()); }
"<"		    { return new_symbol(sym.LT, yytext()); }
"<="		{ return new_symbol(sym.LTE, yytext()); }
"&&"		{ return new_symbol(sym.AND, yytext()); }
"||"		{ return new_symbol(sym.OR, yytext()); }
"=>"		{ return new_symbol(sym.ARROW, yytext()); }
"="		    { return new_symbol(sym.ASSIGN, yytext()); }
";"		    { return new_symbol(sym.SEMICOLON, yytext()); }
":"		    { return new_symbol(sym.COLON, yytext()); }
","		    { return new_symbol(sym.COMMA, yytext()); }
"."		    { return new_symbol(sym.DOT, yytext()); }
"("		    { return new_symbol(sym.LPAR, yytext()); }
")"		    { return new_symbol(sym.RPAR, yytext()); }
"["		    { return new_symbol(sym.LBRCK, yytext()); }
"]"		    { return new_symbol(sym.RBRCK, yytext()); }
"{"		    { return new_symbol(sym.LBRCE, yytext()); }
"}"		    { return new_symbol(sym.RBRCE, yytext()); }


                "//" 		 { yybegin(COMMENT); }
<COMMENT>       .            { yybegin(COMMENT); }
<COMMENT>       "\n"       { yybegin(YYINITIAL); }

"true"              { return new_symbol (sym.BOOL_CONST, true); }
"false"               { return new_symbol (sym.BOOL_CONST, false); }
[a-zA-Z][a-zA-Z0-9_]* 	     { return new_symbol (sym.IDENT, yytext()); }
[0-9]+                       { return new_symbol(sym.NUM_CONST, Integer.parseInt(yytext())); }
'[ -~]'                    { return new_symbol (sym.CHAR_CONST, yytext().charAt(1)); }


.           { System.err.println("Lexical error (" + yytext() + ") on line " + (yyline+1) + "at " + yycolumn); }

