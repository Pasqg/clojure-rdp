# clojure-rdp

A Clojure project to parse generic (right-recursive) grammars.

The main reason behind this project is to experiment with and quickly
prototype grammars with little setup. It is not optimised, thus it is too
slow for production. The resulting parse tree can be used directly in
clojure or converted to json for use somewhere else.

## Usage

To parse the content of a file, assuming that it can be entirely described
with one rule:

```
(clojure-rdp.parser.parser/parse file-name rule-name rules-map)
```

This outputs a parse tree that can be printed in different ways :

```
;full parse tree
(print-tree tree)

;collapsed parse tree (i.e. node with only one children are squashed together)
(print-tree (collapse tree))

;prints a collapsed and compacted json
(print (to-json (parse "hello-word.txt" "program" rules-map)))
```

### Lexer

The 'parse' function also runs the lexer, which will split the input into
tokens using whitespaces (by default space, tab, new line). Then, if the
lexer encounters a special standalone token in a word, it will be split as
a separate token. By default, the following characters are standalone
tokens:

```
[ ] ( ) { } + - * / = ; , > < ? ! "
```

This means that the string "(3 +2)" would first be split into "(3" and "
+2)" using whitespaces and then further decomposed in "(" "3" "+" "2" ")"
using the standalone tokens regex.

## Rules definition

Each rule of the grammar has a name and a list of possible definitions.
Each definition of a rule is in *OR* with the others, and order matters as
the parser will try each definition in order until one matches (greedily).

The elements of each definition are strings which correspond either to
another rule, or to a regex that matches a single token. No explicit way to
identify a terminal token: **all strings that do not match a rule of the
grammar are considered terminal tokens**.

The parser is quite simple and the burden of transforming the
right-recursive syntax tree into a left-recursive one (where needed, for
example standard math expressions), or to avoid ambiguous grammars is left
upon the user.

### Example of rule definition

1. The rule below will match either the token "a" **or** "b".

```
"rule"  '(["a"] ["b"])
```

2. The rule below will match both the tokens "a" **and** "b"

```
"rule"  '(["a" "b"])
```

3. The rule below can match either (1) both the tokens "a" **and** "b",
   or (2) the token "c"

```
"rule"  '(["a" "b"] ["c"])
```

4. The parser is right-recursive, so only rules like "rule1" are allowed,
   while the ones like "rule2" will cause an infinite loop (and a stack
   overflow error).

```
"rule1"  '(["a" "rule1"] ["a"])
"rule2"  '(["rule2" "a"] ["a"])
```

5. Since the parser tries the definitions in order, the "biggest"
   definition must come first. Below, "rule1" matches all consecutive "a",
   but "rule2" will only match one "a" (effectively ignoring the second
   definition):

```
"rule1"  '(["a" "rule1"] ["a"])
"rule2"  '(["a"] ["a" "rule2"])
```

### Examples of grammars

In this section, examples of complete grammars are provided.

### 1. Expression grammar

The definitions below define an expression grammar of the kind
"1 + (3.8 * 7 / (2.1 - 8))". In this grammar, right-recursion might create
an additional burden for operators with "left"-precedence (i.e. subtraction
and division). This can be overcome either by transforming the parsed tree,
or for example by enforcing using of brackets.

```
"expression"   '(["number" "operator" "expression"]
                      ["\\(" "expression" "\\)"]
                      ["number"])
"number"            '(["double-literal"] ["integer-literal"])
"double-literal"    '(["\\d*\\.\\d+"])
"integer-literal"   '(["\\d+"])
"operator"          '(["\\+"] ["-"] ["\\*"] ["/"])
```

For the example expression "1 + (3.8 * 7 / (2.1 - 8))", this produces a (
collapsed) parse tree:

```
+- expression
  +- integer-literal: ["1"]
  +- \+: ["+"]
  +- expression
    +- \(: ["("]
    +- expression
      +- double-literal: ["3.8"]
      +- operator: ["*"]
      +- expression
        +- integer-literal: ["7"]
        +- /: ["/"]
        +- expression
          +- \(: ["("]
          +- expression
            +- double-literal: ["2.1"]
            +- -: ["-"]
            +- integer-literal: ["8"]
          +- \): [")"]
    +- \): [")"]
```

Upon mismatch, the parser will backtrack and try another rule on the same
set of tokens until either a match is found or all rules have been tried.

### 2. Lisp-like grammar

In the next example, the rules definitions express a simple lisp-like
grammar where everything is parsed as a bunch of **forms**. Each **form**
is a **list** that contains **elements**, where each **element** is either
an **atom** (strings, numbers, symbols) or a **list**.

```
   "forms"      '(["form" "forms"] ["form"])
   "form"       '(["\\(" "elements" "\\)"] ["empty-form"])
   "elements"   '(["element" "elements"] ["element"])
   "element"    '(["form"] ["atom"])

   "empty-form" '(["\\(" "\\)"])
   "collection" '()

   "atom"       '(["string"] ["[a-zA-Z\\-][\\-a-zA-Z0-9]*"] ["number"])
   "string"     '(["\"[^\".]*\""])
   "number"     '(["\\d*\\.\\d+"] ["\\d+"])
```

As an example, see the program below:

```
(print "The result of 1+2+3 is " (sum 1 2 3))
(print "Done!")
```

This produces the following parse tree:

```
+- forms
  +- form
    +- \(: ["("]
    +- elements
      +- atom: ["print"]
      +- elements
        +- string: ["\"The result is 1+2+3 is \""]
        +- form
          +- \(: ["("]
          +- elements
            +- atom: ["sum"]
            +- elements
              +- number: ["1"]
              +- elements
                +- number: ["2"]
                +- number: ["3"]
          +- \): [")"]
    +- \): [")"]
  +- form
    +- \(: ["("]
    +- elements
      +- atom: ["print"]
      +- string: ["\"Done!\""]
    +- \): [")"]
```

### 3. Linear programming grammar

As last example, let's create the definition for a grammar for linear
programming (LP) problems.

A LP problem statement requires:

- A function (called 'objective') to optimise
  (either minimise or maximise)
- A set of constraints that define the admissibility region of the optimal
  solution.

We could write the problem as:

```
objective:
    max 3x - y + .8

where:
    y <= 1
    x > 2
    x + y < 4
```

The parse this statement, we can use the following abstract rule, composed
of 2 main rules:

```
"problem"  '(["objective-definition" "constraints-definition"])
```

#### 1. Objective definition

The objective definition is composed by three parts:

- "objective:" token (or "objective" + ":" tokens, depending on the lexer
  configuration)
- "min" or "max" token, to specify whether we want to maximise or minimise
  the objective
- an expression that defines a linear function. This will contain either
  number or variables, and should only accept the operator "+" and "-"

One way to describe it as a rule:

```
"objective-definition"  '(["objective" ":" "min-or-max" "expression"])
"min-or-max"            '(["min"] ["max"])
```  

#### 2. Constraints definition

The constraints definition is composed of:

- "where" ":" tokens (or "where:", again depending on the lexer)
- A set of constraints, each defined as an expression, followed by a
  comparison operator (<, <=, >, >=, =) and then followed by another
  expression.

This boils down to:

```
"constraints-definition" '(["where" ":" "constraints"])
"constraints"           '(["constraint" "constraints"] ["constraint"])
"constraint"            '(["expression" "comp-op" "expression"])
"comp-op"               '(["<" "="] [">" "="] ["<"] [">"] ["="])
```

Note: we could also restrict constraints to the form "expression < number".
This will result in less freedom, but potentially speed up parsing.

#### Final rule set

In addition to the definitions in previous sections, we also need rules to
parse the expressions. This is similar to the example 1 in this document.

Expressions are composed of items and operators (only + or - because of
linearity requirements). Expression items are either number literals (
integers or real numbers) or variables (a string with at least 1 character,
can contain digits but must start with a letter first).

Given that the variables in each expression can have a coefficient
assigned (i.e. 3x), we will also introduce a third item type called
"scaled-variable" which is a single token starting with a number followed
by a variable name.

This can be expressed by the following rules:

```
"expression"            '(["expression-item" "op" "expression"] ["expression-item"])
"expression-item"       '(["scaled-variable"] ["variable"] ["number"])
"scaled-variable"       '(["((\\d*\\.\\d+)|(\\d+))[a-zA-Z][a-zA-Z0-9]*"])
"op"                    '(["\\+"] ["\\-"])

"number"                '(["\\d*\\.\\d+"] ["\\d+"])
"variable"              '(["[a-zA-Z][a-zA-Z0-9]*"])
```

With all the definitions above put together, we can parse the statement
provided as example at the beginning of this section. The resulting tree
is:

```
+- problem
  +- objective-definition
    +- objective: ["objective"]
    +- :: [":"]
    +- max: ["max"]
    +- expression
      +- scaled-variable: ["3x"]
      +- \-: ["-"]
      +- expression
        +- variable: ["y"]
        +- \+: ["+"]
        +- number: [".8"]
  +- constraints-definition
    +- where: ["where"]
    +- :: [":"]
    +- constraints
      +- constraint
        +- variable: ["y"]
        +- comp-op
          +- <: ["<"]
          +- =: ["="]
        +- \d+: ["1"]
      +- constraints
        +- constraint
          +- variable: ["x"]
          +- >: [">"]
          +- \d+: ["2"]
        +- constraint
          +- expression
            +- variable: ["x"]
            +- \+: ["+"]
            +- variable: ["y"]
          +- <: ["<"]
          +- \d+: ["4"]
```

## Error handling

Error handling is very basic at the moment: a generic "Could not parse" is
thrown, or if not all tokens are parsed, a generic "Error passing
after <remaining tokens>" is thrown. Future plans involve the ability to
detect and suggest missing tokens of one or more rules, providing more
detailed and actionable parsing errors.

## License

The code available in this repository is
under [Apache 2.0 License](LICENSE)




