# Text Template Engine

A standalone templating tool that generates text by combining template files with JSON input data and embedded scripting.

## Installation & Usage

### Running the Tool

```powershell
java -cp "C:\Work\3ºano\2º semestre\ELP\code\Projeto\out\production\Projeto;C:\Work\3ºano\2º semestre\ELP\code\Projeto\lib\antlr4-runtime-4.13.2.jar;C:\Users\ruyan\.m2\repository\org\jetbrains\kotlin\kotlin-stdlib\1.9.25\kotlin-stdlib-1.9.25.jar" project.MainKt src\template.txt src\input.json src\output.txt
```

**Arguments:**
- `<template.txt>` — Template file with code blocks delimited by `<rc>` ... `<cr>`
- `<input.json>` — JSON input file containing data to be accessed in templates
- `<output.txt>` — Output file where rendered result is written

### Example

**template.txt:**
```
User Report

Name: <rc> user.name <cr>
Age: <rc> user.age <cr>

<rc>
if user.age >= 18 {
    print "Status: Adult\n"
} else {
    print "Status: Minor\n"
}
<cr>

<rc>
total = 0
count = 0
for item in user.purchases {
    total = total + item.price
    count = count + 1
}
print "Total spent: "
print total
print "\n"
<cr>
```

**input.json:**
```json
{
  "user": {
    "name": "João",
    "age": 25,
    "purchases": [
      { "name": "Book", "price": 15 },
      { "name": "Pen", "price": 3 }
    ]
  }
}
```

**Output:**
```
User Report

Name: João
Age: 25

Status: Adult

Total spent: 18
```

## Template Syntax

### Code Blocks

Code blocks are delimited by `<rc>` and `<cr>`:

**Expression mode** (single line, outputs result):
```
<rc> user.name <cr>
```

**Script mode** (multiple statements, controls output):
```
<cr>
if condition {
    print "text\n"
}
<cr>
```

### Scripting Primitives

#### 1. Property Access
Access JSON object properties using dot notation:
```
<rc> user.name <cr>
<rc> item.price <cr>
<rc> user.address.city <cr>
```

#### 2. Variables & Assignment
Define variables and reuse them across blocks (persistent):
```
<rc>
total = 0
count = 0
for item in items {
    total = total + item.price
    count = count + 1
}
<cr>
```

Variables declared in one block are available in all subsequent blocks.

#### 3. Conditional Statements
```
<rc>
if condition {
    print "true branch\n"
} else {
    print "false branch\n"
}
<rc>
```

#### 4. Loops
Iterate over JSON arrays:
```
<rc>
for item in user.purchases {
    print item.name
    print " - "
    print item.price
    print "\n"
}
<cr>
```

#### 5. Arithmetic & Boolean Expressions
Supported operators:
- **Arithmetic:** `+` `-` `*` `/` `%`
- **Comparison:** `==` `!=` `<` `<=` `>` `>=`

```
<rc> price * quantity <cr>
<rc> if total > 100 { print "High value\n" } <cr>
```

#### 6. Output with `print`
```
<rc>
print "Hello, "
print user.name
print "!\n"
<cr>
```

### String Literals
Strings are enclosed in double quotes with escape sequences:
```
<rc> print "Hello\nWorld\n" <cr>
<rc> print "Price: $" <cr>
```

Supported escapes: `\n` `\t` `\r` `\\` `\"`

## Implementation Notes

- **JSON parsing:** Custom parser (no external libraries)
- **Architecture:** Lexer (ANTLR) → Parser → AST → Interpreter
- **Variable scope:** Environment persists across all template blocks
- **Type system:** Structured JSON values (JObject, JArray, JString, JNumber, JBoolean, JNull)

## Technical Stack

- **Language:** Kotlin
- **Parser:** ANTLR 4
- **Runtime:** JVM (Java 21+)

