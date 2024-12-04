//********************************
//* Firefighter
//*
//* Intro
//*
//* Example Commodore BASIC game for preic BASIC pre-processor
//*
//* Copyright © 2024 Almos Rajnai
//*
//********************************
{#intro}
//Set up screen: background is red, text is orange then clear screen
{@color_background}={%color_black}
{@color_text}={%color_red}
gosub{#reset_screen}

//Firefighter logo
print"         {CBM-A}{SHIFT-*}{CBM-R}{CBM-R}{SHIFT-*}{CBM-R}{SHIFT-*}{CBM-R}{SHIFT-*}{CBM-R}{CBM-R}{SHIFT-*}{CBM-R}{CBM-R}{CBM-R}{SHIFT-*}{CBM-R}{SHIFT-*}{CBM-R}{SHIFT-*}{CBM-S}"
print"         {SHIFT--}{SHIFT-*}{CBM-W}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT-*}{CBM-W}{SHIFT-*}{CBM-W}{SHIFT--}{CBM-A}{CBM-W}{SHIFT--}{CBM-Q}{CBM-S}{SHIFT--}{SHIFT-*}{CBM-W}{SHIFT--}{SHIFT--}"
print"         {SHIFT--}{CBM-A}{CBM-W}{SHIFT--} {CBM-W} {SHIFT--}{CBM-A}{CBM-W}{SHIFT--}{CBM-Z}{CBM-W} {SHIFT--}{SHIFT--}{SHIFT--} {SHIFT--} {CBM-W}"

//Lower half of the logo is in a different color

{@color_text}={%color_orange}
gosub{#set_text_color}

print"         {SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT-*}{CBM-W}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT-*}{CBM-W}{SHIFT--}{SHIFT--}"
print"         {CBM-Z}{CBM-X}{CBM-Z}{CBM-E}{CBM-E}{CBM-E}{SHIFT-*}{CBM-E}{CBM-X}{CBM-Z}{CBM-E}{SHIFT-*}{CBM-E}{CBM-E}{CBM-X}{CBM-Z}{CBM-E}{SHIFT-*}{CBM-E}{CBM-E}{CBM-X}"

{@color_text}={%color_white}
gosub{#set_text_color}

{@text$}="example game for preic tool"
{@cursor_y}=7
gosub{#print_centered}

{@text$}="instructions:"
{@cursor_y}=10
gosub{#print_centered}

print"{down}"
print"put out the fire by water bombing it"
print"from your helicopter. do not crash into the flames."
print"{down}press space to drop water bombs, you"
print"have to wait until it reaches the ground"
print"before dropping another."
print"{down}when you finish with the fire then your"
print"helicopter gains a little speed for the next fire."

{@color_text}={%color_red}
gosub{#set_text_color}

{@text$}="press space to start"
{@cursor_y}=24
gosub{#print_centered}

gosub{#wait_for_space_key}
