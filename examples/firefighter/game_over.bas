//********************************
//* Firefighter
//*
//* Game over
//*
//* Example Commodore BASIC game for preic BASIC pre-processor
//*
//* Copyright © 2024 Almos Rajnai
//*
//********************************
{#game_over}
//Set up screen: background is black, text is red then clear screen
{@color_background}={%color_black}
{@color_text}={%color_red}
gosub{#reset_screen}

//Print header
{@cursor_y}=4
{@text$}="game over"
gosub{#print_centered}

//Print score
{@color_text}={%color_white}
gosub{#set_text_color}
{@cursor_y}=8
{@text$}="your score:"+str$({@score})
gosub{#print_centered}


//Wait for key press
{@color_text}={%color_blue}
gosub{#set_text_color}
{@cursor_y}=24
{@text$}="press space"
gosub{#print_centered}

gosub{#wait_for_space_key}

goto {#intro}
