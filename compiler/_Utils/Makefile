include ../Makefile.inc

.PHONY: clean

all: ../dump ../mkTable ../process

../dump : dump.cpp
	$(CCC) $(CCFLAGS) -o $@ $<

../mkTable : mkTable.cpp
	$(CCC) $(CCFLAGS) -o $@ $<

../process: process.cpp
	$(CCC) $(CCFLAGS) -o $@ $<

clean:
	-rm -f *.o *.d *~

