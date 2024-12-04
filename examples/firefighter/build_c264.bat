java -jar ..\..\bin\preic.jar firefighter.bas -l build\labels.txt -o wj build\preprocessed_firefighter.bas
petcat -w3 -o build\firefighter.prg -- build\preprocessed_firefighter.bas
