//********************************
//* Firefighter
//*
//* Setup
//*
//* Example Commodore BASIC game for preic BASIC pre-processor
//*
//* Copyright © 2024 Almos Rajnai
//*
//********************************

//Try to detect the machine and set up {@screen_chars} and {@screen_colors} variables.
//with screen base addresses accordingly.

{#setup} gosub {#detect_machine}

on {@machine} goto {#machine_c64}, {#machine_c264}, {#machine_c128}
print"error: unrecognised machine.":stop

{#machine_c64}
{@screen_chars} = {%address_vic2_character_screen}
{@screen_colors} = {%address_vic2_color_matrix}
{@gfx_bomb_color} = {%color_vic2_blue}
{@gfx_fighter_color} = {%color_vic2_white}
{@gfx_fire_color_1} = {%color_vic2_orange}
{@gfx_fire_color_2} = {%color_vic2_red}
return

{#machine_c264}
{@screen_chars} = {%address_ted_character_screen}
{@screen_colors} = {%address_ted_color_matrix}
{@gfx_bomb_color} = {%color_ted_blue}
{@gfx_fighter_color} = {%color_ted_white}
{@gfx_fire_color_1} = {%color_ted_orange}
{@gfx_fire_color_2} = {%color_ted_red}
return

{#machine_c128}
print"error: commodore 128 is not supported, sorry."
stop
