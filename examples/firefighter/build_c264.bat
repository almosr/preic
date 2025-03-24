rem Build Firefighter for Commodore 16/116/Plus/4 in release mode

java -jar ..\..\bin\preic.jar firefighter.bas -l build\labels.txt -o 0ijqtw -p $ build\preprocessed_firefighter.bas
petcat -w3 -o build\firefighter.prg -- build\preprocessed_firefighter.bas
