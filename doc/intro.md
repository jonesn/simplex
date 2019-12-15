# Christmas Functional Special 

---

## Consume Multiple Collections

### Java 9+

```java
List<Integer> listA = List.of(1,2,3);
List<Integer> listB = List.of(4,5,6);
List<Integer> listC = List.of(2,3,4);
List<Integer> result = new ArrayList<>(3);
for (int count = 0; count < 3; count ++) {
    result.add(listA.get(count) + listB.get(count) + listC.get(count));
}
System.out.println(result);
```

[7, 10, 13]

### FSharp

```fsharp
let list1 = [1; 2; 3]
let list2 = [4; 5; 6]
let list3 = [2; 3; 4]
let newList = List.map3 (fun x y z -> x + y + z) list1 list2 list3
printfn "%A" newList
```

[7; 10; 13]

### Clojure

```clj
(def list1 [1 2 3])
(def list2 [4 5 6])
(def list3 [2 3 4])
(println (map + list1 list2 list3))
```

(7 10 13)

---

## Taking it Further Function Application

```clj
(apply mapv + [[1 2 3] [4 5 6] [2 3 4]])
```

[7 10 13]

```clj
(apply mapv + [[1 2 3] [4 5 6] [2 3 4] [400 600 700] [800 900 765]])
```

## To Chat About

# To Cover

 - map-indexed
 - apply
 - comp
 - zipmap
 - walk
 - lazyseq
 - juxt
 - memoize

https://theburningmonk.com/2016/12/depth-first-tree-traversal-in-f/

```
Clojure is a dialect of Lisp and Lisp is an old language (the original paper "Recursive Functions of
Symbolic Expressions and Their Computation by Machine" is dated 1960, but research in the direction of
a new language for symbolic expression started around 1957). Initially Lisp adopted a syntax based on
M-expressions, or meta-language expressions, that was not based on lists (as it is now) and looked
something similar to:

λ[[y;z];cons[car[y];cdr[z]]]

The above is a function of two list arguments y,z that produces a new list as output merging (first
y) and (last z) equivalent to (fn [y z] (cons (first y) (last z))) in Clojure.
Sometimes between 1958 and 1959, McCarthy wanted to prove that Lisp was better at expressing
computability than the formalism of the Turing Machine. Part of that challenge was also to define an
"universal Lisp function", a function able to parse and execute another copy of itself written with the
same syntax (exactly like the universal Turing Machine is able to accept a definition of itself).
McCarthy had to find a way to express Lisp functions in a form that could be digested by Lisp itself
and decided to encode them in lists, using the convention that the first element of the list was the name
of the function and the rest of the list the parameters. McCarthy called this new notation an S-expression
(where S stands for Symbolic). The above "cons" M-expression would look like the following as an S-
expression (which is perfectly valid modern Lisp):

(LAMBDA (Y Z) (CONS (CAR Y) (CDR Z)))

The universal function that was able to parse S-expressions and apply them to arguments was called
indeed apply. McCarthy envisioned apply purely for research with no practical scope, until Steve Russel
(one of his graduates) decided to implement apply in machine language, effectively creating the first
Lisp REPL.
```