rem Build Firefighter for Commodore 16/116/Plus/4 in debug mode

java -jar ..\..\bin\preic.jar firefighter.bas -l build\labels.txt -o 0ijtw -p $ -d DEBUG build\preprocessed_firefighter_debug.bas
petcat -w3 -o build\firefighter_debug.prg -- build\preprocessed_firefighter_debug.bas
