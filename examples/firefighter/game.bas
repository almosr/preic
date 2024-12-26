//********************************
//* Firefighter
//*
//* Game
//*
//* Example Commodore BASIC game for preic BASIC pre-processor
//*
//* Copyright © 2024 Almos Rajnai
//*
//********************************
{#game}

//Initial speed delay for the first level
{@fighter_speed} = 5

//Reset score
{@score} = 0

//Start of the level setup
{#next_level}

//Init game
{@fighter_pos} = 3 * 40 + 1
{@fighter_pos_chars} = {@fighter_pos} + {@screen_chars}
{@fighter_pos_colors} = {@fighter_pos} + {@screen_colors}
{@fighter_anim} = 0
{@fighter_counter} = {@fighter_speed}
{@bomb_pos_chars} = 0

//Set up screen: background is black, text is white then clear screen
//Score is displayed at the top-left corner.
{@color_background} = {%color_black}
{@color_text} = {%color_white}
gosub {#reset_screen}
print"score:"
gosub {#print_score}

//Generate random fire
{@fire_counter}=0
for {@fire_x} = 2 to 38
  {@fire_height} = 24 - (int(rnd(0) * 17) + 5)
  {@fire_middle} = 24 - {@fire_height} / 2
  {@fire_counter} = {@fire_counter} + {@fire_height} + 1
  for {@fire_y} = 24 to 24 - {@fire_height} step -1
    {@offset} = {@fire_y} * 40 + {@fire_x}
	if {@fire_y} < {@fire_middle} then goto {#fire_top}
		//Else
		poke{@screen_colors} + {@offset}, {@gfx_fire_color_1}
		goto {#fire_next}

	{#fire_top}
	//Top half of the fire is different color
	poke{@screen_colors} + {@offset}, {@gfx_fire_color_2}

{#fire_next}
    poke{@screen_chars} + {@offset}, {%gfx_fire}
next:next

//This is an important jump here, the main loop is marked as frequently called,
//so it will be moved. The execution will not be flowing to it naturally.
goto {#loop}

//Start of the main loop
#frequent

  {#loop}

  //Print debug info when DEBUG pre-processing flag was set
  #ifdef DEBUG
      {@debug_fighter_y} = int(({@fighter_pos_chars}-{@screen_chars}) / 40)
      {@debug_fighter_x} = {@fighter_pos_chars}-{@screen_chars}-{@debug_fighter_y} * 40
      print"{home}{down}{blue}fighter x:";{@debug_fighter_x};"{left} y:";{@debug_fighter_y};"{left} {white}"
  #endif

  //Draw firefighter
  poke{@fighter_pos_chars}, {%gfx_fighter_tail}

  //Propeller is alternating between two states
  if {@fighter_anim} = 0 then poke{@fighter_pos_chars} - 39, {%gfx_fighter_propeller_1}:{@fighter_anim} = 1:goto {#fighter_skip1}
     //Else
     poke {@fighter_pos_chars} - 39, {%gfx_fighter_propeller_2}:{@fighter_anim} = 0
  {#fighter_skip1}

  poke {@fighter_pos_chars} + 1, {%gfx_fighter_body}
  poke {@fighter_pos_chars} + 2, {%gfx_fighter_head}

  poke {@fighter_pos_colors} + 2, {@gfx_fighter_color}

  //Clear behind fighter
  poke {@fighter_pos_chars} - 1, {%gfx_clear}:poke {@fighter_pos_chars} - 40, {%gfx_clear}

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
      //Else

      //Launch water bomb
  	{@bomb_pos_chars} = {@fighter_pos_chars} + 41
  	{@bomb_pos_colors} = {@fighter_pos_colors} + 41

  	//Lose 5 points from score for each water bomb
  	{@score} = {@score} - 5

  	//Don't go negative with the score, though :)
  	if {@score} < 0 then {@score} = 0
  {#no_bomb}

  gosub {#print_score}

  //Back to game loop
  goto {#loop}

//The end of the main loop
#endfrequent

{#collision}

{@anim_text$} = "you are burning!!!11"
{@anim_bg_color_1} = {%color_black}
{@anim_bg_color_2} = {%color_red}
gosub {#animate_text}

goto {#game_over}


{#win}

//Remove bomb and last piece of fire
poke {@bomb_pos_chars}, {%gfx_clear}
poke {@bomb_pos_chars} - 40, {%gfx_clear}

{@anim_text$} = "fire is gone, next level"
{@anim_bg_color_1} = {%color_black}
{@anim_bg_color_2} = {%color_green}
gosub {#animate_text}

if {@fighter_speed} > 1 then {@fighter_speed} = {@fighter_speed} - 1
goto {#next_level}

//---------------------------
// Routines
//---------------------------

#frequent
  //*** Print score to the top-left corner of the screen
  {#print_score} print "{home}{right}{right}{right}{right}{right}{right} ";{@score};" "
  return
#endfrequent

//*** Animated text with screen flashing
//***
//*** Parameters:
//*** {@anim_text$} - text to print
//*** {@anim_bg_color_1}, {@anim_bg_color_2} - alternating background colors
//***
{#animate_text}
for {@anim_text_x} = 0 to 15
   //Flash background alternating betwen two colors
   if {@anim_text_x} - int({@anim_text_x} / 2) * 2 = 1 then {@color_background} = {@anim_bg_color_1}:goto {#anim_text_set}
   
   {#anim_text_skip}
   {@color_background} = {@anim_bg_color_2}

   {#anim_text_set}
   gosub {#set_background_color}

   //Animated text
   {@cursor_x} = {@anim_text_x}:{@cursor_y} = 12
   gosub {#set_cursor_position}
   print " ";{@anim_text$};
next
return
