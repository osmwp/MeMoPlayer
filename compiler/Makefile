
.PHONY: clean fontMap

all: compiler mkTable process fontMap bml

compiler : mkTable force_look
	cd src/; make all

mkTable : force_look
	cd _Utils/; make all

bml: force_look
	cd _Bml; make all

clean :
	cd src; make clean
	cd _Utils; make clean

fontMap:
	cd fontMap; make

force_look:
	true
