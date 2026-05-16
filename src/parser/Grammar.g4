grammar Grammar;

@header {
package gen_parser;
}

// Script is the top-level construct of the language.
// A script consists of zero or more statements, optionally preceded by formal parameters.
script
    : parameters? statement* EOF
    ;

// Optional formal parameters (not currently used, but reserved for potential extension).
parameters
    : PARAMS LPAREN idList? RPAREN
    ;

// Statements are the executable constructs of the language.
statement
    : assignment
    | printStmt
    | ifStmt
    | forStmt
    | whileStmt
    | breakStmt
    | block
    ;

// An explicit code block with braces.
block
    : LBRACE statement* RBRACE
    ;

// Assignment stores the result of an expression in a named variable.
assignment
    : ID ASSIGN expression
    ;

// Print statement outputs an expression value.
printStmt
    : PRINT expression
    ;

// if/else conditional controls execution flow.
ifStmt
    : IF expression block (ELSE block)?
    ;

// for loop iterates over a JSON array, binding each element to a loop variable.
forStmt
    : FOR ID IN expression block
    ;

// While loop: repeated execution while condition holds.
whileStmt
    : WHILE expression block
    ;

// Break statement: interrupt the nearest loop.
breakStmt
    : BREAK
    ;

// Expressions produce values and follow standard operator precedence.
expression
    : equality
    ;

// Equality: == and !=
equality
    : comparison ((EQUAL | NOT_EQUAL) comparison)*
    ;

// Comparison: <, <=, >, >=
comparison
    : additive ((LESS | LESS_EQUAL | GREATER | GREATER_EQUAL) additive)*
    ;

// Additive: + and -
additive
    : multiplicative ((PLUS | MINUS) multiplicative)*
    ;

// Multiplicative: *, /, %
multiplicative
    : atom ((TIMES | DIV | MOD) atom)*
    ;

// Atoms are the base elements: literals, variables, property access, and grouped expressions.
atom
    : INT
    | STRING
    | ID ('.' ID)*
    | LPAREN expression RPAREN
    ;

// Comma-separated identifier list for formal parameters.
idList
    : ID (COMMA ID)*
    ;


// Punctuation and language keywords.
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
WHILE: 'while';
BREAK: 'break';
ID: [a-zA-Z_][a-zA-Z_0-9]*;
INT: [0-9]+;
STRING: '"' ( '\\' . | ~["\\\r\n] )* '"' ;
WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;