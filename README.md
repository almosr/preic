# preic

Commodore BASIC source code pre-processor tool

## Introduction

After staying away from Commodore BASIC for more than 30 years, I returned to it for a small game project. Not like it
would make much sense to implement anything in BASIC anymore, it was more like a fun challenge to myself.

While I was thinking about setting up a cross-compiler environment
using [petcat](https://techtinkering.com/articles/tokenize-detokenize-commodore-basic-programs-using-petcat/) I realised
how much programming experience involved since I typed in my first BASIC program into my good old Commodore 16.

The first thing I missed was comments in the source code. Maybe I am just too old, but I need these little piece of
hints about my own code otherwise it takes too long to understand what does it do when I come back to it after a week or
so. That was easy to solve by simply doing a [grep](https://www.geeksforgeeks.org/grep-command-in-unixlinux/) on the
sources before I fed it into `petcat` for producing the actual executable.

Then the more I used BASIC the more I grew annoyed by its limitations due to ancient design. Dealing with line numbers
is a pain, variable names are limited to two characters, source code must be in one file and so on.

At the end I decided that I need a pre-processing tool that solves these issues by producing a BASIC-compliant source
code out of more flexible input files. And that is how the idea of `preic` was born.

Why would anybody need this tool? I am not quite sure, to be honest. It might come handy when you want to participate in
some retro BASIC competition.

### Tool features

This simple tool addresses the following limitations of Commodore BASIC:

* **Dealing with line numbers** - source code will be auto-numbered, no need to specify line numbers at the beginning of
  each line. So, no problem inserting a new line anymore, and you don't have to remember numbers.
* **No comments apart from REM command** - while it is possible to put comments into BASIC program using
  the [REM](https://www.c64-wiki.com/wiki/REM) command, but that is completely useless and waste of resources at
  runtime. Pre-processing simply strips comments from the source code, so you can add as many as you want.
* **Variable name limits** - while longer variable names are accepted by Commodore BASIC, only the first two characters
  are considered as the real variable name. This is really annoying when you want to use meaningful variable names.
  So the tool can be used to specify variables with longer names.
* **No way to define constants** - dealing with certain constants, like hardware register addresses is painful in BASIC,
  you either waste variables store these or keep typing in the obscure numbers.
  Well, no longer, you can define your own constants with pre-processing.
* **No file includes** - BASIC source is inherently consists of one long file, cannot be split up into more manageable
  chunks unless you keep loading individual programs at runtime.
  Pre-processing allows you to include any source files from any path, this helps you to split up your source or build a
  reusable routine library.

Keep mind, that you still have to code according to BASIC rules. Apart from formatting the source code everything else
remains the same: commands, limitations, memory usage, execution flow handling, etc...

Here is an example of a processed BASIC source (partial):

```
...
20 ifbo=0thengoto23
21 ifpeek(bo)<>32thensc=sc+1:fp=fp-1:iffp=0thengoto29
22 pokebo,83:pokebo-40,32:pokebp,gi:bo=bo+40:bp=bp+40:ifbo-sd>1000thenpokebo-40,32:bo=0
23 fo=fo-1:iffo<>0thengoto25
24 fo=fi:fk=fk+1:fl=fl+1:ifpeek(fk+2)=160thengoto28
25 getke$:ifke$<>" "orbo<>0thengoto27
...
```

And here is the original source that was fed into pre-processing:

```
...
if {@bomb_pos_chars} = 0 then goto {#skip_bomb}
   //Else

   //Have the bomb hit fire?
   if peek({@bomb_pos_chars}) <> {%gfx_clear} then {@score} = {@score} + 1:{@fire_counter} = {@fire_counter} - 1:if {@fire_counter} = 0 then goto {#win}

   //Draw bomb
   poke {@bomb_pos_chars}, {%gfx_bomb}
   poke {@bomb_pos_chars} - 40, {%gfx_clear}
   poke {@bomb_pos_colors}, {@gfx_bomb_color}
   {@bomb_pos_chars} = {@bomb_pos_chars} + 40
   {@bomb_pos_colors} = {@bomb_pos_colors} + 40

   //Have the bomb reached the bottom of the screen?
   if {@bomb_pos_chars} - {@screen_chars} > 1000 then poke {@bomb_pos_chars} - 40, {%gfx_clear}:{@bomb_pos_chars} = 0
{#skip_bomb}

//Move firefighter
{@fighter_counter} = {@fighter_counter} - 1
if {@fighter_counter} <> 0 then goto {#no_move}
	//Else

	//Move firefighter
	{@fighter_counter} = {@fighter_speed}
    {@fighter_pos_chars} = {@fighter_pos_chars} + 1
    {@fighter_pos_colors} = {@fighter_pos_colors} + 1
	
	//Check new position whether firefighter hits the fire
	if peek({@fighter_pos_chars} + 2) = {%gfx_fire} then goto {#collision}

{#no_move}

//Read keyboard
get {@key$}

//Is it space to drop 
if {@key$} <> {%key_bomb} or {@bomb_pos_chars} <> 0 then goto {#no_bomb}
...
```

I am sure we can agree that the original source is much more manageable.

For more complex examples have a look at the included [examples](./examples) folder.

## Compatibility

The goal is to make the pre-processing compatible with [Commodore BASIC versions](https://www.c64-wiki.com/wiki/BASIC).
The processed source code must remain compatible with `petcat` tokenizer (see below).

*On a side note:* this tool does not try to interpret the source code beyond a very simple text structure. Probably
other flavour of BASIC source code can be fed into it and would produce a meaningful output for those too.

## Prerequisites

For running the tool you need a Java 17-compatible Java virtual machine.

## How to use it

You can run the tool binary JAR file from command line using Java:

```shell
java -jar preic.jar <input basic source file> [-l <label list file>] [-o <optim flags>] [output pre-processed file]
```

Parameters are:

- `<input BASIC source file>` - BASIC source file to be pre-processed.
- `-l <label list file>` - optional path to a file for label definition dump.
- `-o <optim flags>` - optimisation flags, when set then the relevant processing will be completed on the output:
    * `w` - remove white space from lines where not required, white space remains unchanged after `REM` command and
      inside strings.
    * `j` - join BASIC lines, when set then processing attempts to join as many lines as safely possible. Longer and
      fewer lines make the program run faster.
      _Warning_: since the tool does not interpret the source, this optimization could cause runtime issues with some
      specific source code.
- `[output pre-processed file]` - optional output pre-processed file, default is `stdout`.

The tool does not output an executable program, pre-processed source code should be tokenized. As mentioned in this
document already, I
recommend [petcat](https://techtinkering.com/articles/tokenize-detokenize-commodore-basic-programs-using-petcat/)
from [VICE](https://sourceforge.net/projects/vice-emu/) project for that purpose.

### Source code controls

Pre-processing is going to modify the input source code various ways. All the features of the tool are optional, it is
up to you whether you want to make use of it or not.

Below you will find the description of all of these features:

#### White space

Indenting the source code helps to understand structure of the execution. While it could be done by using colon (
`:`) character at the beginning of the line in BASIC, but that is actually decreasing the program performance at
runtime.

Processing is going to strip all whitespace characters from the beginning and end of each source code line, so feel
free to indent your source code in any way you like.

Additionally, you can turn on optimisation for removing all unnecessary white space from the processed output. See
parameters for more details.

#### Comments

You can add C++-style comment to any line, the comment starts with double forward slash (`//`) characters and the rest
of the line will be ignored by processing. For example:

```
//Let's increase variable A
a=a+1
b=b+1 //Variable B was also increased
```

#### Auto-numbering lines

Dealing with line numbers of a BASIC program is a hassle. Some of the Commodore BASIC versions have a command (
`RENUMBER`) that makes it easier to insert new lines in between already numbered lines, but remembering the numbers is
still inconvenient.

Processing is going to take over this burden, you don't have to number the lines. Auto-numbering starts from 0 and
increases the line numbers by 1 to keep the program compact. If you want a specific line number then you can start the
line with the number and auto-numbering picks up that line number from that line.

#### Line label

You don't have to deal with line numbers (if you choose not to), but you still want to refer to certain lines in your
source code.
Let's say for jumping to a line using `GOTO` or `GOSUB` commands you need to know the target line number.

For this purpose you can define line labels to any line that can be used instead of the actual number in your source
code as a reference.
The line label definition must be added to the beginning of the target line, in the following format:
`{#line label name}`.
Then you can use the same label in the same format anywhere in your source code and pre-processing is going to replace
it with the actual line number.

For example this input:

```
{#forever_loop} print "hello world"
goto {#forever_loop}
```

Is going to produce this source output:

```
0 print "hello world"
goto 0
```

You may also combine specific line numbers with the line labels, in this case the line number should be at the beginning
of the line.

#### Variable label

Naming variables in Commodore BASIC is particularly cumbersome. While the interpreter accepts longer variable names,
it considers the first two characters from the name only for identifying the variable. This behaviour leads to nasty
surprises. Also, finding a suitable name for your variable using two characters only is really annoying.

Pre-processing allows you to define your own variables using virtually any length for the name. The variable label
definition can be added to anywhere in the code, in the following format: `{@variable label name}`. The first instance
creates the variable and any further reference to the same name will always be referring to the same variable.

In practice a two character variable name will be assigned automatically to the variable label.

**Warning:** mixing variable labels and actual variables in the code is not advisable, variable name could be re-used
and that could lead to extremely hard to find errors.

Here is an example input:

```
{@wild_west}=10
{@wild_flower}=20
print {@wild_west};" ";{@wild_flower}
```

Which will be turned into this source code:

```
0 wi=10
1 wh=20
2 print wi;" ";wh
```

And the output will be:

```
10 20
```

If you tried the same using BASIC without pre-processing:

```
0 wildwest=10
1 wildflower=20
2 print wildwest;" ";wildflower
```

Then the output would be:

```
20 20
```

The processing is trying to maintain the first two characters of the variable name if possible. When there is a variable
with those two characters assigned already then it increases the second character and tries to find a new name. When
second characters run out then increases the first character and goes on.

Debugging the pre-processed code could be a bit of a challenge, though. See
the [debugging section below](#debugging-the-processed-code).

In case you need string variable when variable name is postfixed with dollar sign (`$`) or integer variable when name is
postfixed with percentage sign (`%`) then you can add the postfix to the variable label.

For example:

```
{@pear$}="fruit"
{@pear%}=42
{@pear}=3.14
print {@pear$};{@pear%};{@pear}
```

Would be turned into:

```
0 pe$="fruit"
1 pe%=42
2 pe=3.14
3 print pe$;pe%;pe
```

Then the output would be:

```
fruit 42  3.14
```

#### Literal label

Many BASIC programs need a few odd constants. These can be obscure memory addresses for `POKE` commands, or just some
values the program has to deal with multiple times. When these numbers change or has to be remembered then it could
get complicated.

The processing allows you to define virtually any string as constant, better call it string literal. These literals
will only appear in your program when you actually use them, so you can define any number of them freely without any
punishing memory consumption or side effect. The literal label definition can be added to anywhere in the code, in the
following format:
`{%literal label name}=value`, where `value` is the string literal you want to use as a replacement for the literal
label. The rest of the line will be set as value after the first equal sign (`=`) in the line. Only whitespace is
allowed between the label name definition and the equal sign.

The literals will not be checked regarding the format of the value, anywhere where the literal label appears the value
will be inserted instead. It is possible to define a piece of BASIC code as literal label and then re-use it multiple
times in shorter form. (Sort of like macros in other languages, but without parameters at the moment.)

Literal labels cannot be re-defined, when a literal definition already exists anywhere in the source code and defined
again then processing stops with an error.

Here is an example input for using a literal label:

```
{%background_color}=53281
{%white}=1
{%red}=2
poke {%background_color},{%red}
```

Which will be turned into this source code:

```
0 poke 53281,2
```

As you can see, the literal label for `white` does not appear in the processed source code in any form because it was
defined, but not used anywhere.

#### Including files

Nobody likes looking at long source code, it is hard to understand and maintain. Breaking up the source code into
smaller chunks that are implementing one specific function makes it easier to code.

With BASIC previously it was not possible to edit parts of the code in separate chunks. Processing gives you this handy
tool into your hands. It can be utilised for building routine or constant libraries also that can be shared across
multiple source code.

If you want to include any source code then use the `#include` directive in a separate line followed by the relative or
absolute path of the target file. The file will be processed as it would be part of the including source code.

It is possible to use the include directive in an included file, there are no limits how deep you would go in the rabbit
hole. There is also no check for recursion, so be careful not to include the already included file again directly or
indirectly.

### Debugging the processed code

Debugging your code after it was turned into actual BASIC program is not simple because the whole structure might change
completely. You can find line numbers and variables associated with your labels in label list file. Use the `-l` command
line option to specify the name for the file where the list will be dumped.

I found helpful to put `REM` commands to specific lines (even temporarily), so you could find your way in the
processed code. The `REM` commands remain untouched by pre-processing.

Breaking up your code into individual files that contain a specific part could also help. By simply commenting out the
`#include` directive you can skip the entire code that would run from that file.

Typical mistake is to mix up the line label (`{#...}`) with the variable label (`{@...}`). When you use line label
instead of variable that usually produces a compile or runtime error. However, the other way around a variable could be
interpreted as a line number for a `GOTO` instruction for example. When that happens then the execution would jump to
the line that is calculated from the value stored in the variable. Quite often an unused variable contains 0, so the
program restarts immediately and most likely it would just keep repeating this in a loop.

Turn off optimisations while debugging your program. Optimised code is much harder to read and follow. You can turn
those options on when your program is stable.

## Examples

You can find examples in [examples](/examples) folder, for compiling these sources to actually working programs you will
need [petcat](https://techtinkering.com/articles/tokenize-detokenize-commodore-basic-programs-using-petcat/) command
line tool.

Two Windows batch files were included:

* `build_c64.bat` - compiles the example into a program that can be loaded into Commodore 64.
* `build_c264.bat` - compiles the example into a program that can be loaded into Commodore 16, 116 or Plus/4.

The examples demonstrate how to make use of the pre-processing features of `preic`.

## License

`preic` and all included examples are licensed under MIT license, see included [LICENSE](./LICENSE) file for details.