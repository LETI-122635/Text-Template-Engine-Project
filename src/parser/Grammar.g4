grammar Grammar;

@header {
  package project;
}


script
    : parameters? statement* EOF
    ;

parameters
    : PARAMS LPAREN idList? RPAREN
    ;

statement
    : assignment SEMI
    | printStmt SEMI
    | breakStmt SEMI
    | ifStmt
    | whileStmt
    | forStmt
    | block
    ;

block
    : LBRACE statement* RBRACE
    ;

assignment
    : ID ASSIGN expression
    ;

printStmt
    : PRINT expression
    ;

breakStmt
    : BREAK
    ;

ifStmt
    : IF expression block (ELSE block)?
    ;

whileStmt
    : WHILE expression block
    ;

forStmt
    : FOR ID IN expression block
    ;

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

idList
    : ID (COMMA ID)*
    ;


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
SEMI: ';';
PARAMS: 'params';
PRINT: 'print';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
BREAK: 'break';
FOR: 'for';
IN: 'in';
ID: [a-zA-Z_][a-zA-Z_0-9]*;
INT: [0-9]+;
STRING: '"' ( '\\' . | ~["\\\r\n] )* '"' ;
WS: [ \t\r\n]+ -> skip;
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;