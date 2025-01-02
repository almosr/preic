This small example demonstrates how to use Preic tool and `#include` directive with binary files.

It embeds a Commodore 64 character set called `Chars 02` from this page:
http://home-2002.code-cop.org/c64/font_04.html

I assume this character set is public domain.

This example must be compiled for either Commodore 64 or Commodore 16/116/Plus/4. The same source produces the different
outputs using conditional compiling. The output is not compatible with both machines at runtime, so choose the right
one:

* build_c64.bat - for Commodore 64
* build_c264.bat - for Commodore 16/116/Plus/4

__Please note__: the program requires 64KB memory, so it won't run on stock Commodore 16 and 116 with 16KB memory.