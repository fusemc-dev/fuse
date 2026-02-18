# PQL

> Fucking **Path Query Language**
>
> Not to be taken seriously, but we needed some sort of DSL
> to index into NBT structures, and the existing `NbtPath`
> was too command-centric.
>
> Since we must create our own, why not make a SQL pun out of it?

## 1. Prelude

**PQL** (A "[Path Query Language](https://en.wikipedia.org/wiki/SQL)") defines a straightforward language for querying NBT structures. A PQL
expression, also known as a **path**, is a sequence of slash-separated
**segments** that together describe a specific traversal procedure
of an NBT structure.

A segment can be **resolved** individually; thus a path traverses
an NBT structure by recursively resolving multiple segments 
against one another.

## 1.1. Grammar

The following section defines the grammar of PQL expressions in the EBNF notation.
This specification specifically uses the [ISO 14977](https://www.cl.cam.ac.uk/~mgk25/iso-14977.pdf) standard
for EBNF expressions.

## 1.1.1. Path

A **path** is defined as a sequence of one or more **segments** separated by a forward solidus (`/`).

```ebnf
Path = Segment { '/' Segment }
```

## 1.1.2. Segment

A **segment** is defined as either an **identifier**, or _another segment_ followed by
a **subscript** operator. A subscript operator is defined as an **integer literal** circumfixed
by a set of square brackets (`[]`). An integer literal is defined as a sequence of one or more
consecutive **arabic numerals**.

```ebnf
Segment = Identifier | ( Segment Subscript )
Subscript = '[' Integer ']'
Integer = ArabicNumeral { ArabicNumeral }
```

The above-described grammar naturally allows for recursive subscript applications.

## 1.1.3. Identifier

An **identifier** is defined as a sequence of one or more consecutive **characters**. A character
is defined as either a latin letter, an arabic numeral, or an underscore (`_`).

```ebnf
Identifier = Character { Character }
Character = Letter | ArabicNumeral | '_'

Letter = 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g' | 'h' | 'i' | 'j' | 'k' | 'l' | 'm'
    | 'n' | 'o' | 'p' | 'q' | 'r' | 's' | 't' | 'u' | 'v' | 'w' | 'x' | 'y' | 'z'
    | 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G' | 'H' | 'I' | 'J' | 'K' | 'L' | 'M'
    | 'N' | 'O' | 'P' | 'Q' | 'R' | 'S' | 'T' | 'U' | 'V' | 'W' | 'X' | 'Y' | 'Z'
ArabicNumeral = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
```
