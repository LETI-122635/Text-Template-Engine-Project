grammar Grammar;

@header {
package project;
}


// A template script is a sequence of statements, optionally preceded by params.
script
    : parameters? statement* EOF
    ;

// Optional formal parameters for script-like inputs.
parameters
    : PARAMS LPAREN idList? RPAREN
    ;

// Only the language constructs needed by the template engine are allowed here.
statement
    : assignment
    | printStmt
    | ifStmt
    | forStmt
    | block
    ;

block
    : LBRACE statement* RBRACE
    ;

// Assignment stores a computed expression into a named variable.
assignment
    : ID ASSIGN expression
    ;

// print emits the value of an expression into the rendered output.
printStmt
    : PRINT expression
    ;

// if/else controls whether a nested block executes.
ifStmt
    : IF expression block (ELSE block)?
    ;

// for iterates over a JSON array using one loop variable.
forStmt
    : FOR ID IN expression block
    ;

// Expressions follow standard precedence from equality down to atoms.
expression
    : equality
    ;

equality
    : comparison ((EQUAL | NOT_EQUAL) comparison)*
    ;

comparison
    : additive ((LESS | LESS_EQUAL | GREATER | GREATER_EQUAL) additive)*
    ;

additive
    : multiplicative ((PLUS | MINUS) multiplicative)*
    ;

multiplicative
    : atom ((TIMES | DIV | MOD) atom)*
    ;

atom
    : INT
    | STRING
    | ID ('.' ID)*
    | LPAREN expression RPAREN
    ;

// Comma-separated identifiers used by the optional params rule.
idList
    : ID (COMMA ID)*
    ;


// Punctuation and keywords used by the language.
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
ASSIGN: '=';
EQUAL: '==';
NOT_EQUAL: '!=';
PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
MOD: '%';
LESS: '<';
LESS_EQUAL: '<=';
GREATER: '>';
GREATER_EQUAL: '>=';
COMMA: ',';
PARAMS: 'params';
PRINT: 'print';
IF: 'if';
ELSE: 'else';
FOR: 'for';
IN: 'in';
ID: [a-zA-Z_][a-zA-Z_0-9]*;
INT: [0-9]+;
STRING: '"' ( '\\' . | ~["\\\r\n] )* '"' ;
WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;