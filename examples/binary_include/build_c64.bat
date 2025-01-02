rem Build binary include example for Commodore 64

java -jar ..\..\bin\preic.jar binary_include.bas -d C64 -p $ build\preprocessed_binary_include.bas
petcat -w2 -o build\binary_include.prg -- build\preprocessed_binary_include.bas
