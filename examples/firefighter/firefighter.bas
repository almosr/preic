//********************************
//* Firefighter
//*
//* Main file
//*
//* Example Commodore BASIC game for preic BASIC pre-processor
//*
//* Copyright © 2024 Almos Rajnai
//*
//********************************

rem *** firefighter
rem *** example game for preic tool
rem *** see included readme for details

gosub{#setup}

rem **** intro
#include intro.bas

rem **** game
#include game.bas

rem **** game over
#include game_over.bas

rem **** routines
#include constants.bas
#include setup.bas
#include tools.bas
#include system.bas

