SRCS := $(wildcard *.cpp)
OBJS = $(SRCS:.cpp=.o)
LIBS = -L. 
CCC=g++
CCFLAGS=-g

all: ../scripter

%.o : %.cpp
	$(CCC) -c $(CCFLAGS) $< -o $@ 

.PHONY: clean

COMMON_OBJS=XMLTokenizer.o FileTool.o Trace.o
SCRIPTER_OBJS=Scripter.o $(COMMON_OBJS)

../scripter: $(SCRIPTER_OBJS)
	$(CCC) $(CCFLAGS) $(SCRIPTER_OBJS)  -o ../scripter

clean:
	rm ${OBJS} scripter
