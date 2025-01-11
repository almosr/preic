//********************************
//* Firefighter
//*
//* Tools
//*
//* Example Commodore BASIC game for preic BASIC pre-processor
//*
//* Copyright © 2024 Almos Rajnai
//*
//********************************

//***
//*** Detect machine
//***
//*** This routine tries to detect the machine by reading a specific
//*** address from memory that contains the same value after reset.
//***
//*** Returns result in {@machine} variable:
//***   {@machine} = {%machine_c64} (= 1)  - Commodore 64
//***   {@machine} = {%machine_c264} (= 2) - Commodore 16, 116 or Plus/4
//***   {@machine} = {%machine_c128} (= 3) - Commodore 128
//***   {@machine} = {%machine_unknown} (= 4) - unrecognised machine
//***
{%machine_c64} = 1
{%machine_c264} = 2
{%machine_c128} = 3
{%machine_unknown} = 0

{#detect_machine} {@machine} = peek(65532)

if {@machine} = 226 then {@machine} = {%machine_c64} : return
if {@machine} = 246 then {@machine} = {%machine_c264} : return
if {@machine} = 61 then {@machine} = {%machine_c128} : return

{@machine}={%machine_unknown} : return

//*** Color constants for various functions
{%color_black} = 1
{%color_white} = 2
{%color_red} = 3
{%color_green} = 4
{%color_blue} = 5
{%color_orange} = 6


//***
//*** Set text color to a specific state
//***
//*** This routine sets the text (uses by print command) colors to
//*** a specific state.
//*** Colors in parameters should be specified using constants:
//***   {%color_black} - black
//***   {%color_white} - white
//***   {%color_red} - red
//***   {%color_green} - green
//***   {%color_blue} - blue
//***   {%color_orange} - orange
//***
//*** Parameters:
//***   {@color_text} - color of printed text.
//***
#function set_text_color, color_text
on {@color_text} goto {#set_text_color_black}, {#set_text_color_white}, {#set_text_color_red}, {#set_text_color_green}, {#set_text_color_blue}, {#set_text_color_orange}

{#set_text_color_black}
print"{black}";
return

{#set_text_color_white}
print"{white}";
return

{#set_text_color_red}
print"{red}";
return

{#set_text_color_green}
print"{green}";
return

{#set_text_color_blue}
print"{blue}";
return

{#set_text_color_orange}
print"{orange}";
return


//*** Set screen background color to a specific state
//***
//*** This routine sets the screen background and border colors to
//*** a specific state.
//*** Colors in parameters should be specified using constants:
//***   {%color_black} - black
//***   {%color_white} - white
//***   {%color_red} - red
//***   {%color_green} - green
//***   {%color_blue} - blue
//***   {%color_orange} - orange
//***
//*** Parameters:
//***   {@color_background} - color of background and border.
//***
#function set_background_color, color_background
on {@color_background} goto {#set_screen_bg_black}, {#set_screen_bg_white}, {#set_screen_bg_red}, {#set_screen_bg_green}, {#set_screen_bg_blue}, {#set_screen_bg_orange}
return

{#set_screen_bg_black}
if {@machine}={%machine_c64} then poke{%register_vic2_background_color},{%color_vic2_black}:poke{%register_vic2_border_color},{%color_vic2_black}
if {@machine}={%machine_c264} then poke{%register_ted_background_color},{%color_ted_black}:poke{%register_ted_border_color},{%color_ted_black}
return

{#set_screen_bg_white}
if {@machine}={%machine_c64} then poke{%register_vic2_background_color},{%color_vic2_white}:poke{%register_vic2_border_color},{%color_vic2_white}
if {@machine}={%machine_c264} then poke{%register_ted_background_color},{%color_ted_white}:poke{%register_ted_border_color},{%color_ted_white}
return

{#set_screen_bg_red}
if {@machine}={%machine_c64} then poke{%register_vic2_background_color},{%color_vic2_red}:poke{%register_vic2_border_color},{%color_vic2_red}
if {@machine}={%machine_c264} then poke{%register_ted_background_color},{%color_ted_red}:poke{%register_ted_border_color},{%color_ted_red}
return

{#set_screen_bg_green}
if {@machine}={%machine_c64} then poke{%register_vic2_background_color},{%color_vic2_green}:poke{%register_vic2_border_color},{%color_vic2_green}
if {@machine}={%machine_c264} then poke{%register_ted_background_color},{%color_ted_green}:poke{%register_ted_border_color},{%color_ted_green}
return

{#set_screen_bg_blue}
if {@machine}={%machine_c64} then poke{%register_vic2_background_color},{%color_vic2_blue}:poke{%register_vic2_border_color},{%color_vic2_blue}
if {@machine}={%machine_c264} then poke{%register_ted_background_color},{%color_ted_blue}:poke{%register_ted_border_color},{%color_ted_blue}
return


{#set_screen_bg_orange}
if {@machine}={%machine_c64} then poke{%register_vic2_background_color},{%color_vic2_orange}:poke{%register_vic2_border_color},{%color_vic2_orange}
if {@machine}={%machine_c264} then poke{%register_ted_background_color},{%color_ted_orange}:poke{%register_ted_border_color},{%color_ted_orange}
return


//***
//*** Reset screen
//***
//*** This routine sets up screen colors and clear text.
//*** Colors in parameters should be specified using constants:
//***   {%color_black} - black
//***   {%color_white} - white
//***   {%color_red} - red
//***   {%color_green} - green
//***   {%color_blue} - blue
//***   {%color_orange} - orange
//***
//*** Parameters:
//***   {@color_text} - color of printed text.
//***   {@color_background} - color of background and border.
//***
#function reset_screen, color_text, color_background
gosub{#set_background_color}
gosub{#set_text_color}
print"{clr}";
return


//***
//*** Set cursor position
//***
//*** Moves text cursor to a specific position on the screen
//*** relative to the top-left corner of the screen.
//***
//*** Parameters:
//***   {@cursor_x} - cursor horizontal position (0-24)
//***   {@cursor_y} - cursor vertical position (0-39)
//***
//*** Note: parameter variables will be set to 1, parameters are
//***       not validated against expected range.
//***
#function set_cursor_position, cursor_x, cursor_y
print"{home}";

//This is really slow, but the only compatible way with BASIC v2
for{@cursor_x}={@cursor_x} to 1 step -1:print"{right}";:next
for{@cursor_y}={@cursor_y} to 1 step -1:print"{down}";:next
return


//***
//*** Print centered
//***
//*** Prints a given text centered in a specific row.
//***
//*** Parameters:
//***   {@cursor_y} - cursor vertical position (0-39)
//***   {@text$} - text to print
//***
//*** Note: cursor_y is not validated against expected range.
//***       Value of center_x and center_y variables will be set to 1.
//***
#function print_centered, cursor_y, text$
{@cursor_x}=20-len({@text$})/2
gosub {#set_cursor_position}
print {@text$};
return


{#wait_for_space_key}
//First wait until none of the keys are pressed
{#wait_for_space_key_loop}
get{@key$}:if {@key$}<>"" then goto {#wait_for_space_key_loop}

//Then wait for one more keypress
{#wait_for_space_key_loop_space}
get{@key$}:if {@key$}<>" " then goto {#wait_for_space_key_loop_space}
return