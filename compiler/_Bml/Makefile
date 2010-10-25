include ../Makefile.inc

SRCS=$(wildcard *.cpp)
OBJS=$(SRCS:.cpp=.o);
HDRS=$(SRCS:.cpp=.h);

.PHONY: clean

all: ../bml

%.o: %.cpp %.h
	$(CCC) $(CCFLAGS) -c -o $@ $<

../bml : $(OBJS)
	$(CCC) $(CCFLAGS) -o $@ $(OBJS)

clean:
	-rm -f *.o *.d *~

