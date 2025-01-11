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
#call reset_screen, {%color_red}, {%color_black}

//Print header
#call print_centered, 4, "game over"

//Print score
#call set_text_color, {%color_white}
#call print_centered, 8, "your score:" + str$({@score})

//Wait for key press
#call set_text_color, {%color_blue}
#call print_centered, 24, "press space"

gosub{#wait_for_space_key}

goto {#intro}
