# preic

Pre-processor tool for Commodore BASIC cross-development that introduces some modern language features and
optimisations.

## Introduction

After staying away from Commodore BASIC for more than 30 years, I returned to it for a small game project. Not like it
would make much sense to implement anything in BASIC anymore, it was more like a fun challenge to myself.

While I was thinking about setting up a cross-compiler environment
using [petcat](https://techtinkering.com/articles/tokenize-detokenize-commodore-basic-programs-using-petcat/) I realised
how much programming experience evolved since I typed in my first BASIC program into my good old Commodore 16.

The first thing I missed was comments in the source code. Maybe I am just too old, but I need these little piece of
hints about my own code otherwise it takes too long to understand what does it do when I come back to it after a week or
so. That was easy to solve by simply doing a specific [grep](https://www.geeksforgeeks.org/grep-command-in-unixlinux/)
run on the sources that filtered out all comments before I fed it into `petcat` to produce the actual executable.

Then the more I used BASIC the more I grew annoyed by its limitations due to its ancient design. Dealing with line
numbers is a pain, variable names are limited to two characters, source code must be in one file, this list kept growing
day by day.

At the end I decided that I need a pre-processing tool that solves these issues by producing a BASIC-compliant source
code out of more flexible input files. And that is how the idea of `preic` was born.

Why would anybody need this tool? I am not quite sure, to be honest. It might come handy when you want to participate in
some retro BASIC competition.

## Tool features

This simple tool addresses the following limitations of Commodore BASIC:

* **Dealing with line numbers** - source code will be auto-numbered, no need to specify line numbers at the beginning of
  each line. So, no problem inserting a new line anymore, and you don't have to remember numbers.
* **No comments apart from REM command** - while it is possible to put comments into BASIC program using
  the [REM](https://www.c64-wiki.com/wiki/REM) command, but that is completely useless and waste of resources at
  runtime. Pre-processing simply strips comments from the source code, so you can add as many as you want. You can also
  remove the `REM` commands if you want them to be gone from the final program.
* **Variable name limits** - while longer variable names are accepted by Commodore BASIC, only the first two characters
  are considered as the real variable name. This is really annoying when you want to use meaningful variable names.
  So the tool can be used to specify variables with longer names.
* **No way to define constants** - dealing with certain constants, like hardware register addresses is painful in BASIC,
  you either waste variables store these or keep typing in the obscure numbers.
  Well, no longer, you can define your own constants with pre-processing.
* **No hexadecimal numbers in BASIC program** - sometimes you just don't want to convert hexadecimal numbers to decimal.
  Especially when those refer to some very specific address like hardware registers. Pre-processing can convert these
  numbers for you when produces the final source code.
* **No file includes** - BASIC source inherently consists of one long file, cannot be split up into more manageable
  chunks unless you keep loading individual programs at runtime.
  Pre-processing allows you to include any source files from any path, this helps you to split up your source or build a
  reusable routine library.

Keep mind, that you still have to code according to BASIC rules. Apart from formatting the source code everything else
remains the same: commands, limitations, memory usage, execution flow handling, etc...

Here is an example of a processed BASIC source (partial with all optimisations turned on):

```
...
3 pokea+1,113:pokea+2,81:pokec+2,i:pokea-1,32:pokea-40,32:ifd=.then6
4 ifpeek(d)<>32thene=e+1:f=f-1:iff=.then27
5 poked,83:poked-40,32:pokeg,j:d=d+40:g=g+40:ifd-h>1000thenpoked-40,32:d=.
6 k=k-1:ifkthen8
7 k=l:a=a+1:c=c+1:ifpeek(a+2)=160then26
8 geta$:ifa$<>" "ord<>.then10
...
```

And here is the original source that was fed into pre-processing:

```
...
poke {@fighter_pos_chars} + 1, {%gfx_fighter_body}
poke {@fighter_pos_chars} + 2, {%gfx_fighter_head}

poke {@!fighter_pos_colors} + 2, {@gfx_fighter_color}

//Clear behind fighter
poke {@fighter_pos_chars} - 1, {%gfx_clear}:poke {@fighter_pos_chars} - 40, {%gfx_clear}

if {@bomb_pos_chars} = 0 then goto {#skip_bomb}
   //Else

   //Has the bomb hit fire?
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

*On a side note:* this tool does not try to interpret the source code beyond basic textual structure. Probably other
flavours of BASIC source code can be fed into it and would produce a meaningful output for those too.

## Prerequisites

For running the tool you need a Java 17-compatible Java virtual machine.

## How to use it

You can run the tool binary JAR file from command line using Java:

```shell
java -jar preic.jar <input basic source file> [-l <label list file>] [-o <optim flags>] [-p <processing flags>] [-d <pre-processing flag name>] [output pre-processed file]
```

Parameters are:

- `<input BASIC source file>` - BASIC source file to be pre-processed.
- `-d <pre-processing flag name>` - optional definition of a pre-processing flag that will be set as existing at the
  beginning of the processing.
- `-l <label list file>` - optional path to a file for label definition dump.
- `-ld <library dir>` - optional path to a directory where included files will be searched also. This parameter makes it
  possible to use a collection of routines or definitions from a generic folder outside the current project.
- `-p <processing flags>` - optional processing flags, when set then relevant processing will be completed on the
  output:
    * `$` - convert hexadecimal numbers to decimal, hexadecimal numbers should be prefixed with double dollar signs
      (`$$`). String literals are not considered with this processing, hexadecimal numbers inside string literals will
      also be converted. Both upper or lowercase letters and leading zeroes can be used in the hexadecimal number, any
      character that is not a hexadecimal digit terminates the number.
    * `v` - use as many one character long name as possible instead of trying to keep any resemblance with the original
      variable names.

- `-o <optim flags>` - optional optimisation flags, when set then relevant processing will be completed on the output:
    * `0` - replace `0` numeric literals by `.` that is parsed faster by the interpreter. Only numeric `0` will be
      replaced, 0 character inside string literals or after `REM` commands will not be affected.
    * `i` - simplify variable comparison against non-zero in IF statement to the variable itself. This optimisation is
      applied to simple variable checks only without any arithmetics inside the `IF` statement.
    * `j` - join BASIC lines, when set then processing attempts to join as many lines as safely possible. Longer and
      fewer lines make the program run faster.
    * `r` - remove `REM` BASIC commands from source to make it run faster and occupy less memory.
    * `t` - remove `GOTO` command after `THEN` and `ELSE` commands which is unnecessary for jumping to a line.
    * `w` - remove white space from lines where not required, white space remains unchanged after `REM` command and
      inside strings.

  _Warning_: since the tool does not interpret the source, optimisations could cause runtime issues with some specific
  source code.
- `[output pre-processed file]` - optional output pre-processed file, default is `stdout`.

The tool does not output an executable program, pre-processed source code should be tokenized. As mentioned in this
document already, I
recommend [petcat](https://techtinkering.com/articles/tokenize-detokenize-commodore-basic-programs-using-petcat/)
from [VICE](https://sourceforge.net/projects/vice-emu/) project for that purpose.

### Source code controls

Pre-processing is going to modify the input source code various ways. All the features of the tool are optional, it is
up to you whether you want to use them or not.

Below you will find the description of all of these features:

#### White space

Indenting the source code helps to understand structure of the execution. While it could be done by using colon (`:`)
character at the beginning of the line in BASIC, but that is actually decreasing the program performance at runtime.
Same goes to spaces between BASIC commands and parameters, not needed at runtime, but makes the source code more
readable.

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

Dealing with line numbers of a BASIC program is a hassle. Some of the Commodore BASIC versions have a command
(`RENUMBER`) that makes it easier to insert new lines between already numbered lines, but remembering the numbers is
still inconvenient.

Processing is going to take over this burden, you don't have to number the lines. Auto-numbering starts from 0 and
increases the line numbers by 1 to keep the program compact. If you want a specific line number then you can start the
line with the number and auto-numbering picks up that line number starting from that line.

#### Hexadecimal numbers

When PEEK/POKE is used in a BASIC program the referred memory address has to be in decimal format since the interpreter
cannot deal with hexadecimal numbers. This is rather annoying when someone already got used to for example dealing with
hardware registers in assembly and the decimal form is hard to interpret.

By using the `$` processing flag the tool converts all hexadecimal numbers into decimal, so no need to keep these
numbers in decimal form in the source code. Hexadecimal numbers must be prefixed with double dollar signs (`$$`).

For example changing the background colour to black on Commodore 64:

```
poke $$D021, 0
```

That will be changed by processing into:

```
poke 53281, 0
```

#### Line label

You don't have to deal with line numbers (if you choose not to), but you still want to refer to certain lines in your
source code.
Let's say for jumping to a line using `GOTO` or `GOSUB` commands you need to know the target line number.

For this purpose you can define line labels as a reference to any line that can be used instead of the actual number in
your source code.
The line label definition must be added to the beginning of the target line, in the following format:
`{#line label name}`.
Then you can use the same label in the same format anywhere in your source code and pre-processing is going to replace
it with the actual line number.

For example this input:

```
{#forever_loop} print "hello world"
goto {#forever_loop}
```

Is going to produce this output source:

```
0 print "hello world"
goto 0
```

You may also combine specific line numbers with the line labels, in this case the line number should be at the beginning
of the line.

#### Variable label

Naming variables in Commodore BASIC is particularly cumbersome. While the interpreter accepts longer variable names,
it considers only the first two characters from the name for identifying the variable. This behaviour leads to nasty
surprises. Also, finding a suitable name for your variable using two characters only is really annoying.

Pre-processing allows you to define your own variables using virtually any length for the name. The variable label
definition can be added to anywhere in the code, in the following format: `{@variable label name}`. The first instance
creates the variable and any further reference to the same name will always be referring to the same variable.

In practice a two character variable name will be assigned automatically to the variable label and persisted for the
entire source code.

**Warning:** mixing variable labels and actual variables in the code is not advisable, a variable name could be re-used
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

When you need string variable then according to the BASIC language rules the variable name should be postfixed with
dollar sign (`$`) or for an integer variable postfixed with percentage sign (`%`). In this case you can add the postfix
to the variable label.

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

While this format is not required, it is easier to read and understand than adding the postfix after the closing curly
bracket, like: `{$pear}$="fruit"`. However, this format is also supported.

Commodore BASIC does not maintain a table or hash of variable addresses in memory, it crawls through the entire list of
variables to find the one it is looking for. Therefore, resolving a variable name every time when the program is
referring to it is rather slow. Ideally, frequently used variables are defined early in the program, so these would be
found faster. This can be done manually by creating the variables at the beginning of the program, but keeping in mind
which variables are used frequently and maintaining that list is rather painful.

Pre-processing offers you a way to solve this optimisation: when you start the name of the variable with an exclamation
mark (`!`) then processing flags the variable as frequently used and adds the creation of the variable to the very first
line of the program automatically. No need to start the name with exclamation mark every time you refer to the variable,
it is enough to signal this property once anywhere in the program.

Combined with processing flag `v` the variables that are marked as frequently used will have one character long name be
assigned to it if possible. This way dealing with these variables will be a little bit faster. In this case it is
advisable to specify the type of the variable inside the label (e.g. `{@key$}` instead of `{@key}$`), so variable names
can be re-used for different types.

**Please note:** when you use `CLR` command (that removes all previously defined variables) then the effect of this
latter optimisation will be lost unless you jump back to the beginning of the program. If you need to do this for any
reason then better to use the `RUN` command.

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
again then processing stops with an error. When a literal label is used (instead of defined) at the beginning of the
line then it must not be followed by an equal (`=`) sign.

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

Literal labels can be included in other literal labels, so complex structures can be defined easily. For example:

```
//Define target address
{%SYSTEM_COLOR_KEYS_BANK}=2041

//Define command literal label that includes the target address as literal label
{%SET_SYSTEM_COLOR_KEYS_BANK_TO_RAM}=poke{%SYSTEM_COLOR_KEYS_BANK},0

//Add execution of command to program
{%SET_SYSTEM_COLOR_KEYS_BANK_TO_RAM}
```

The output for this code fragment will be:

```
poke2041,0
```

When literal labels are referring to each other than it is possible to end up with an infinite recursion. When the tool
processes the same line more than 100 times and still not finished with the labels then it throws an error because most
likely the labels are replacing each other in an infinite recursion.

## Controlling file pre-processing

More sophisticated projects require specific rules that fine-control the included source code files. Modern compilers
offer a number of features, like conditional compiling which help setting up a well-defined project structure that is
producing a number of outputs reliably without source code changes.

### Including files

Nobody likes looking at long source code, it is hard to understand and maintain. Breaking up the source code into
smaller chunks that are implementing one specific function makes it easier to code.

With BASIC previously it was not possible to edit parts of the code in separate chunks. Processing gives you this handy
tool into your hands. It can be utilised for building routine or constant libraries also that can be shared across
multiple source code.

If you want to include any source code then use the `#include` directive in a separate line followed by the relative or
absolute path of the target file after space character. The file will be processed as it would be part of the including
source code.

It is possible to use the include directive in an included file, there are no limits how deep you would go in the rabbit
hole. There is also no check for recursion, so be careful not to include the already included file again directly or
indirectly.

The `#include` directive can be used for injecting binary data into the source code also, the complete form of the
directive is:

```
#include [[code|data|remark|print],][start offset,[end offset,]<path to the file>
```

First optional parameter specifies the format of the included file:

* `code` - include the file as source code (text) file;
* `data` - include the file as binary and turn the bytes into `DATA` lines;
* `remark` - include the file as binary and turn the bytes into raw data in memory as part of the source code after a
  `REM` BASIC command.
* `print` - include the file as binary and turn the bytes into `PRINT` BASIC commands that can be used to print the
  binary to the screen memory.

When this parameter is not specified then the default will be `code`.

When second and third optional parameters are specified then these are used as start and end offset inside the file.
Anything before the start offset and from the end offset will be thrown away, so it is possible to include only a
specific part of the file. In case only the end offset is needed either set start offset to 0 or just use an empty start
offset with a comma character. Start offset must be in range _0 to size of the file - 1_, end offset must be in
range _start offset + 1 to size of the file_.

When `code` format is specified then offset parameters cannot be used.

Last parameter is path to the included file, the rest of the line will be interpreted as path, white space at the
beginning and end will be ignored. **Note**: the path must not contain comma (`,`) characters. Make sure you don't try
to include a huge binary, it must fit into the available memory on the target machine and there are no checks built into
the tool for that.

The `remark` and `print` formats need some further explanation: these can be used to store the binary data in more
dense format than `DATA` lines and manipulate them directly using machine code or ROM KERNAL functions. I leave it to
your imagination how these formats could be utilised. See the [examples](#examples) for a simple demonstration.

#### Binary as remark

Using `remark` method binary data can be stored in the Commodore BASIC program after `REM` commands without any
transformation. The `REM` command is followed by an opening double-quote to escape the data. Listing the program might
not work properly because these bytes will be displayed as some garbled content. If you are able to locate the line in
memory then you can use the start address of the binary data and process it further.

Two limitations apply to this format:

1. It cannot contain zero byte value because that breaks the BASIC line processing;
2. Size of data in one BASIC line is maximised to 256 bytes, when the included binary is larger than that then it will
   be broken up into multiple consecutive lines.

##### Binary as printed data

With `print` method binary data is stored using `PRINT` Commodore BASIC commands. In this form the data cannot be used
immediately, it has to be printed to the screen and then accessed via screen memory to get back the original form.
Storing the data this way is much less convenient than using `REM` commands, however it can also contain zero byte
values.

The only limitation to this format is the length of one chunk of data for each `PRINT` command: maximised to 128 bytes,
when the included binary is larger than that then it will be broken up into multiple consecutive lines.

### Conditional processing

Conditional processing includes-excludes certain part of the source code depending on the existence of a specific
pre-processing flag.
This feature comes handy when you want to produce different output for various purposes from the same source code files.
For example, you want a debug version of your program while developing it, but you don't want to slow down the released
version of the program or output log messages.

To mark a part of your source code conditional you need to use the `#ifdef` - `#else` - `#endif` directives. The opening
directive `#ifdef` takes one argument that is the name of a pre-processing flag. Anything between the first two
directives will be added or thrown away depending on the existence of the flag at the time of processing. The `#else`
part will be used when the flag doesn't exist at the time of processing.

The `#else` directive is optional, required only when you need to react on non-existence of the specified flag.

A conditional processing structure must be finished in the same file where it was started.

For example:

```
#ifdef DEBUG
  {%debug_print_score}=gosub{#debug_print_score}
#else
  {%debug_print_score}=
#endif

...
{@score}={@score}+10
{%debug_print_score}
...

#ifdef DEBUG
  {#debug_print_score} print "Score: ";{@score}:return
#endif
```

This example calls the `debug_print_score` subroutine every time when `{%debug_print_score}` literal label is added to
the source, but only when `DEBUG` pre-processing flag is set. Otherwise, it does nothing to the processed source code
because the literal label is defined as empty and the score-printing subroutine will not be added at all.

There are two ways to define a pre-processing flag:

- Use the `-d` command line parameter where you can specify a flag to be set at the beginning of the processing. This
  parameter can be used multiple times to define more than one flag.
- Use the `#define` directive in your source code to set a flag when that line is processed. __Note__: when that line is
  excluded by conditional processing then the flag will not be set.

The name of the pre-processing flag may contain any characters except white space, but the names are case-sensitive.

This directive and command line option are not defining macros like in C language, only set a pre-processing flag. If
you are looking for macros then have a look at [literal labels](#literal-label) instead.

Finally, in some special circumstances you might want to remove an already defined pre-processing flag. For that case
use the `#undef` directive with the name of the flag and when this line is processed then from that point on the flag
will be considered undefined.

### Functions with parameters

One of the painful aspects of calling a subroutine in Commodore BASIC programs is passing parameters to the called
routine. The only way to do this is by loading the passed data to specific variables that are expected by the
subroutine. This means you have to remember (or look up) which variables should be used for each subroutine.

To make calling a subroutine easier pre-processing offers you a directive pair that solves passing the parameters with
some grace. To define a function use the `#function` directive and pass in the name (line label) of the subroutine and
also the list of variables you would use for passing parameters. Format is the following:

```
#function <line name>[,<parameter1 name>][,<parameter2 name>]...
```

Where `line name` will be used to define a line label for the function. Optional parameter names are used as
variable names for passing parameters to the function. (No need for `#` character for the line name or `@` character for
the parameter variables.)
The order of the parameters will be used when calling the method to assign values to the parameters. Line and parameter
names must not contain comma (`,`) character. The function will be called as subroutine, so it must finish with `RETURN`
command.

After declaring the function you can call it from anywhere in your source code by using the `#call` directive:

```
#call <line name>[,<parameter1 value>][,<parameter 2 value>]...
```

This directive generates initialisation code for each parameter and then adds a `GOSUB` command with the target to
the `line name` as line label.

The number of parameters must match the function definition. Parameter values must not contain comma (`,`) character.

An example for the function definition and calling:

```
{@stock}=42
#call print_value "stock", {@stock}
end

#function print_value label$,value
print {@label$};": ";{@value}
return
```

Will be turned into this code:

```
0 st=42
1 la$="stock"
2 va=st
4 gosub 6
5 end
6 print la$;": ";va
return
```

In this example one of the parameter was a string literal that is loaded into a string variable, the other one was a
numeric variable that is loaded to the parameter numeric variable.

### Move frequently called lines to the front

Commodore BASIC does not maintain a list or hash of line addresses. When a program jumps to a line or calls a
subroutine then the interpreter starts searching for the line number from the beginning of the program following the
lines until it finds the target. As a consequence when a line is located somewhere at the end of the program then
jumping to it takes much longer than targeting a line at the beginning of the program.

A simple way of optimising your BASIC program is by moving often called subroutines to the beginning of the program.
Obviously, this could be done manually, but rather inconvenient to have a bunch of subroutines or the main loop at the
beginning of the source code rather than where it belongs logically in your source code structure.

Pre-processing offers you a simple way to complete this specific optimisation by marking the frequently called functions
in your code. Then while processing the source code it rearranges the code sections.

You can start a frequently called section by adding this pre-processing directive: `#frequent` to a line before and
close the section by adding `#endfrequent` after it. Any code between these two lines will be moved to the front of the
program. Make sure the section is self-contained: includes line labels for jumping into it and ways to leave the section
without running out of it. It could contain one or more subroutines which end with `RETURN` commands or `GOTO` commands
that jumps to a different part of your program.

Any section marked as frequently called should not contain another section that is also marked the same way. Each
section must be closed in the same file where it was started.

There is no limit on the size of the frequently called sections, however having too many of them or marking large
sections might defeat the purpose of this optimisation. Use it wisely, testing the speed of your code might help you to
decide which part should be marked as frequent.

**Important**: when multiple sections are marked as frequently called then the order of these sections at the beginning
of the program will be arbitrary. Do not count on any side effects of the ordering, it could change together with your
code or when a new tool version is released.

An example for the section reordering, original source code:

```
{#loop}
  {@score}={@score}+10
  gosub {#print_score}
goto {#loop}

#frequent
  {#print_score} print"{home}Score:";{@score}:return
#endfrequent
```

And the outputted program:

```
0 goto 2
1 print"{home}Score:";{@score}:return
2 sc=sc+10
3 gosub 1
4 goto 2
```

Obviously, this example is not particularly useful, it is just a demonstration of the feature.

Debugging a restructured program could be difficult. I would recommend marking specific lines with `REM` command that
can be recognised in the restructured code and can be removed automatically by using `r` optimisation flag.

## Debugging the processed code

Debugging your code after it was turned into actual BASIC program is not simple because the whole structure might change
completely. You can find line numbers and variables associated with your labels in label list file. Use the `-l` command
line option to specify the name for the file where the list will be dumped.

I found helpful to put `REM` commands to specific lines (even temporarily), so you could find your way in the
processed code. The `REM` commands remain untouched by pre-processing, unless you specify the `r` optimisation flag that
instructs the processing to remove these commands.

Breaking up your code into individual files that contain a specific part could also help. By simply commenting out the
`#include` directive you can skip the entire code that would run from that file.

You can make use of conditional processing to make your debug commands included for later use without disrupting the
released version.

Typical mistake is to mix up the line label (`{#...}`) with the variable label (`{@...}`). When you use line label
instead of variable that usually produces a compiling or runtime error. However, the other way around a variable could
be
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