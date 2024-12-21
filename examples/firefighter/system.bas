//********************************
//* Firefighter
//*
//* System constants
//*
//* Example Commodore BASIC game for preic BASIC pre-processor
//*
//* Copyright © 2024 Almos Rajnai
//*
//********************************

//*** Registers

  //TED (Commodore 264 series)
  {%register_ted_background_color}=$$FF15
  {%register_ted_border_color}=$$FF19

  //VIC-II (Commodore 64)
  {%register_vic2_background_color}=$$D021
  {%register_vic2_border_color}=$$D020

//*** Colors

  //TED (Commodore 264 series)
  {%color_ted_black} =  $$00
  {%color_ted_white} =  $$71
  {%color_ted_red} =    $$32
  {%color_ted_green} =  $$35
  {%color_ted_blue} =   $$46
  {%color_ted_orange} = $$48

  //VIC-II (Commodore 64)
  {%color_vic2_black} =  0
  {%color_vic2_white} =  1
  {%color_vic2_red} =    2
  {%color_vic2_green} =  5
  {%color_vic2_blue} =   6
  {%color_vic2_orange} = 8

//*** Memory addresses

  //TED (Commodore 264 series)
  {%address_ted_color_matrix} = $$0800
  {%address_ted_character_screen} = $$0C00

  //VIC-II (Commodore 64)
  {%address_vic2_color_matrix} = $$D800
  {%address_vic2_character_screen} = $$0400
