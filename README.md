# clojure-rdp

A Clojure library to parse generic (right-recursive) grammars.

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
"double-literal"    '(["\\d*\\.\\d*"])
"integer-literal"   '(["\\d*"])
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

In the next example, the grammar describes a subset of Lisp. Everything is
parsed as a bunch of **forms**. Each **form** is a **list** that contains**
elements**, where each **element** is an **atom** (strings, numbers,
symbols) or another **list**.

```
   "forms"      '(["form" "forms"] ["form"])
   "form"       '(["\\(" "elements" "\\)"] ["empty-form"])
   "elements"   '(["element" "elements"] ["element"])
   "element"    '(["form"] ["atom"])

   "empty-form" '(["\\(" "\\)"])
   "collection" '()

   "atom"       '(["string"] ["[a-zA-Z\\-][\\-a-zA-Z0-9]+"] ["number"])
   "string"     '(["\"[^\".]*\""])
   "number"     '(["\\d*\\.\\d*"] ["\\d*"])
```

As an example, see the program below:

```
(print "The result is 1+2+3" (sum 1 2 3))
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
        +- string: ["\"The result is 1+2+3\""]
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

## Error handling

Error handling is very basic at the moment: a generic "Could not parse" is
thrown, or if not all tokens are parsed, a generic "Error passing
after <remaining tokens>" is thrown. Future plans involve the ability to
detect and suggest missing tokens of one or more rules, providing more
detailed and actionable parsing errors.

## License

The code available in this repository is
under [Apache 2.0 License](LICENSE)




