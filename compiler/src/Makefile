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

all: $(TARGET) $(DEPS) $(MEMOPLAYER_DIR)/memoplayer/NodeTable.java

../fontExtractor: fontExtractor.o
	gcc -o fontExtractor fontExtractor.o $(FE_LDFLAGS)


NodeTable.inc: NodeTable.def ../mkTable
	../mkTable NodeTable.def NodeTable.inc

ExternCalls.inc: ExternCalls.def ../mkTable
	../mkTable ExternCalls.def ExternCalls.inc externCallsDef 

$(TARGET) : NodeTable.inc ExternCalls.inc $(OBJS) 
	$(CCC) -o $(TARGET) $(OBJS)

$(MEMOPLAYER_DIR)/memoplayer/NodeTable.java : NodeTable.def
	$(TARGET) -d $(MEMOPLAYER_DIR)/memoplayer/NodeTable.java

copy-to-sdk : $(SDK_DIR)/compiler $(SDK_DIR)/NodeTable.def $(SDK_DIR)/ExternCalls.def

$(SDK_DIR)/compiler: $(TARGET)
	if test -d $(SDK_DIR); then cp -f $(TARGET) $(SDK_DIR)/compiler; fi

$(SDK_DIR)/NodeTable.def: NodeTable.def
	if test -d $(SDK_DIR); then cp -f NodeTable.def $(SDK_DIR)/NodeTable.def; fi

$(SDK_DIR)/ExternCalls.def: ExternCalls.def
	if test -d $(SDK_DIR); then cp -f ExternCalls.def $(SDK_DIR)/ExternCalls.def; fi


clean:
	-rm *.o *.d *~ $(TARGET)

-include $(INC_DEPS)
