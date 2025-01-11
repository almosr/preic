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
#call reset_screen, {%color_red}, {%color_black}

//Firefighter logo
print"         {CBM-A}{SHIFT-*}{CBM-R}{CBM-R}{SHIFT-*}{CBM-R}{SHIFT-*}{CBM-R}{SHIFT-*}{CBM-R}{CBM-R}{SHIFT-*}{CBM-R}{CBM-R}{CBM-R}{SHIFT-*}{CBM-R}{SHIFT-*}{CBM-R}{SHIFT-*}{CBM-S}"
print"         {SHIFT--}{SHIFT-*}{CBM-W}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT-*}{CBM-W}{SHIFT-*}{CBM-W}{SHIFT--}{CBM-A}{CBM-W}{SHIFT--}{CBM-Q}{CBM-S}{SHIFT--}{SHIFT-*}{CBM-W}{SHIFT--}{SHIFT--}"
print"         {SHIFT--}{CBM-A}{CBM-W}{SHIFT--} {CBM-W} {SHIFT--}{CBM-A}{CBM-W}{SHIFT--}{CBM-Z}{CBM-W} {SHIFT--}{SHIFT--}{SHIFT--} {SHIFT--} {CBM-W}"

//Lower half of the logo is in a different color
#call set_text_color, {%color_orange}

print"         {SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT-*}{CBM-W}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT--}{SHIFT-*}{CBM-W}{SHIFT--}{SHIFT--}"
print"         {CBM-Z}{CBM-X}{CBM-Z}{CBM-E}{CBM-E}{CBM-E}{SHIFT-*}{CBM-E}{CBM-X}{CBM-Z}{CBM-E}{SHIFT-*}{CBM-E}{CBM-E}{CBM-X}{CBM-Z}{CBM-E}{SHIFT-*}{CBM-E}{CBM-E}{CBM-X}"

#call set_text_color, {%color_white}

#call print_centered, 7, "example game for preic tool"
#call print_centered, 10, "instructions:"

print"{down}"
print"put out the fire by water bombing it"
print"from your helicopter. do not crash into the flames."
print"{down}press space to drop water bombs, you"
print"have to wait until it reaches the ground"
print"before dropping another."
print"{down}when you finish with the fire then your"
print"helicopter gains a little speed for the next fire."

#call set_text_color, {%color_red}

#call print_centered, 24, "press space to start"

gosub{#wait_for_space_key}
