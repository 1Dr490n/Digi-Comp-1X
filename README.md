# Digi-Comp 1X
The [Digi-Comp 1](https://en.wikipedia.org/wiki/Digi-Comp_I), sometimes considered to have been the first home computer, was a children's toy produced by E.S.R., Inc. in the 1960s.
The original consists of three flip-flops and six logic bars, made entirely of plastic. Despite that simplicity you can create a variety of programs on it. This emulator is heavily inspired by the original computer but allows much more complex and large programs with an indefinite amount of flip-flops and logic bars as well as a proper output system.

Here is the `Checkout` program from the official manual, written in the notation of this emulator:
```
; declare registers
#regs ABC, D

; run program for six clock cycles
#cycles 6

; define output
#out bin, ABC, D

; toggle D to output every tick
set D !D
res D D

; if B then set A.
set A B
; if not B then reset A.
res A !B

set B C
res B !C

set C !A
res C A
```

Of course this runs just as well on an original Digi-Comp 1. However, this version allows many other programs like a "Hello world" program, a four bit adder and a fibonacci sequence generator. You can find these in the examples folder.

# Notation
The program is divided into two parts: the directives and the code.

## Directives
Directives are meta-data only used in the initialization process. They cannot change the current state of the computer.
All directives must start with a `#` and be at the beginning of the program. These are the directives currently supported:

| Name      | Example              | Description                                                                              |
|-----------|----------------------|------------------------------------------------------------------------------------------|
| `#cycles` | `#cycles 60`         | Define how many clock cycles your program needs.                                         |
| `#regs`   | `#regs ABC, M..P, Z` | Declare registers used in the program.                                                   |
| `#pre`    | `#pre A=1, M..P=10`  | Give registers initial values.                                                           |
| `#out`    | `#out dec, ABCD, Z`  | Output a number (ABCD) every time a register (Z) changes<br/>in a specific format (dec). |

Some directives read a list of registers. These can either be written out (ABCD) or as a range (A..D).
In the `#pre` and `#out` directives you can use one list of registers for a single number.
When done so, every register represents one bit. The rightmost register is the least significant bit, the leftmost the most significant bit.
So the `#pre` example above sets A, M and O to 1, N and P to 0.

### Output formats

| Format | Example |
|--------|---------|
| dec    | 65\n    |
| bin    | 10001\n |
| ascii  | A       |


## Code
Code lines are always in this format:
```
<operation> <register> <condition>
set A A!BC
```
This line translates to "Set A if A is true, B is false and C is true".
Next to `set` there's also the `res` (reset) operation.

In addition to those two original Digi-Comp 1 operations, this emulator also supports so called `pull`-operations.
Normally a register cannot be set and reset at the same time. That's where pull-operations come in handy:
```
set A B
pres A
```
If B is true, A is set through the first line. As A is already set within this cycle, it cannot be reset in the next line.
But because we use the `pres` (pull reset) operation there, no error is thrown and the operation is instead simply ignored.
If however B was false, A would be reset because the first line doesn't do anything. So these lines translate to "Set A to B".

# Future plans
- A proper UI/debugger
- Shortcuts for common operations
- A pixel plotter
- User input