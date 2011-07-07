include ../Makefile.inc

SRCS=XmlReader.cpp bml.cpp
OBJS=$(SRCS:.cpp=.o)
HDRS=$(SRCS:.cpp=.h)

ifeq ($(TARGET), cygwin)
BML_LDFLAGS= -liconv
else ifeq ($(TARGET), macos)
BML_LDFLAGS= -liconv
else
BML_LDFLAGS=
endif

.PHONY: clean

all: ../bml

%.o: %.cpp %.h
	$(CCC) $(CCFLAGS) -c -o $@ $<

../bml : $(OBJS)
	$(CCC) $(CCFLAGS) -o $@ $(OBJS) $(BML_LDFLAGS)

clean:
	-rm -f *.o *.d *~

