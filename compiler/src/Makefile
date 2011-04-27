include ../Makefile.inc

SRCS := $(wildcard *.cpp)
INC_DEPS := $(wildcard *.d)
TARGET=../compiler
OBJS=$(SRCS:.cpp=.o);
DEPS=$(SRCS:.cpp=.d)

%.d: %.cpp
	@echo generating dependency for $@
	$(CCC) -MM $(CPPFLAGS) $< | sed 's/:/ $@:/g' > $@

%.o: %.cpp
	@echo compiling $@ width $(CCFLAGS)
	$(CCC) $(FE_CCFLAGS) $(CCFLAGS) -c -o $@ $<

.PHONY: clean tar library

all: $(TARGET) $(DEPS)

../fontExtractor: fontExtractor.o
	gcc -o fontExtractor fontExtractor.o $(FE_LDFLAGS)

NodeTable.inc: NodeTable.def ../mkTable
	../mkTable NodeTable.def NodeTable.inc

ExternCalls.inc: ExternCalls.def ../mkTable
	../mkTable ExternCalls.def ExternCalls.inc externCallsDef 

$(TARGET) : NodeTable.inc ExternCalls.inc $(OBJS) 
	$(CCC) -o $(TARGET) $(OBJS)

clean:
	-rm *.o *.d *~ $(TARGET)

-include $(INC_DEPS)
