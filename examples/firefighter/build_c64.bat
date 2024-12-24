rem Build Firefighter for Commodore 64 in release mode

java -jar ..\..\bin\preic.jar firefighter.bas -l build\labels.txt -o wj -p $ build\preprocessed_firefighter.bas
petcat -w2 -o build\firefighter.prg -- build\preprocessed_firefighter.bas
