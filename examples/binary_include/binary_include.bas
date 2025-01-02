//********************************
//* Binary include
//*
//* Example Commodore BASIC code for preic BASIC pre-processor
//*
//* Copyright © 2025 Almos Rajnai
//*
//********************************

{%endOfData} = -1

gosub {#setup}

{#loop}
  read {@a}
  if {@a} = {%endOfData} then end

  //When PETSCII 27 (Escape) is coming then wait instead of printing the character
  if {@a} = 27 then gosub {#delay}: goto {#loop}

  print chr$({@a});

  //Delay printing a little bit
  gosub {#delay}

goto {#loop}

{#delay}
//Delay between typed characters with some variance, so it looks more natural.
for {@i} = 0 to int( rnd(0) * 50) + 30: next: return


//------------------------------
//Include text from a Commodore program file that starts with the loading address,
//so remove the address (2 bytes at the beginning) and put the rest into the source code as DATA lines.
{#text}
#include data, 2, data/text.prg

//Terminate the loaded text data
data {%endOfData}


//------------------------------
{#setup}

//Change background colour to black
#ifdef C64
  poke $$D020, 0
  poke $$D021, 0
#endif

#ifdef C264
  color 0,1,0
  color 4,1,0
#endif

//Clear the screen show loading message at the bottom
print"{clr}{blue}";
print"please wait..."

//Set the print colour to black, so printed binary won't be visible
print"{black}";

//Include character set from a Commodore program file that also starts with the loading address and put
//into the source code as PRINT lines.
#include print, 2, data/charset.64c

//Change print colour back to white
print"{white}";

//Screen memory and target character address are located on different addresses on C64 and C264 machines,
//characters are printed from second line on the screen.
#ifdef C64
  {%screenAddress} = $$0428
  {%characterAddress} = $$3800
#endif

#ifdef C264
  {%screenAddress} = $$0C28
  {%characterAddress} = $$F000
#endif

//Copy printed characters from screen to memory where the character set data should be.
//This could be done much more elegantly, of course, this is a very simple and slow solution.
for {@i} = 0 to 511
  poke {%characterAddress} + i, peek({%screenAddress} + {@i})
next

print"{clr}";

//When finished then switch to the new charset
#ifdef C64
	poke $$D018, 31
#endif

#ifdef C264
  poke $$FF12, 8
  poke $$FF13, $$F0
#endif

return
