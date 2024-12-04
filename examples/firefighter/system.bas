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
  {%register_ted_background_color}=65301
  {%register_ted_border_color}=65305

  //VIC-II (Commodore 64)
  {%register_vic2_background_color}=53281
  {%register_vic2_border_color}=53280

//*** Colors

  //TED (Commodore 264 series)
  {%color_ted_black} = 0
  {%color_ted_white} = 113
  {%color_ted_red} = 50
  {%color_ted_green} = 53
  {%color_ted_blue} = 70
  {%color_ted_orange} = 72

  //VIC-II (Commodore 64)
  {%color_vic2_black} = 0
  {%color_vic2_white} = 1
  {%color_vic2_red} = 2
  {%color_vic2_green} = 5
  {%color_vic2_blue} = 6
  {%color_vic2_orange} = 8

//*** Memory addresses

  //TED (Commodore 264 series)
  {%address_ted_color_matrix} = 2048
  {%address_ted_character_screen} = 3072

  //VIC-II (Commodore 64)
  {%address_vic2_color_matrix} = 55296
  {%address_vic2_character_screen} = 1024
