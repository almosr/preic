rem Build binary include example for Commodore 16/116/Plus/4

java -jar ..\..\bin\preic.jar binary_include.bas -d C264 -p $ build\preprocessed_binary_include.bas
petcat -w3 -o build\binary_include.prg -- build\preprocessed_binary_include.bas
