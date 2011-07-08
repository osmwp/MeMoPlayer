TARGET ?= linux

ifeq ($(TARGET), cygwin)
 # Windows configuration
 # Cygwin 1.7 require a bigger stack size ?!
 CCFLAGS=-Wl,--stack,8388608
 CCC=g++
else ifeq ($(TARGET), macos)
 # Mac configuration
 CCFLAGS=-m32 -Wall
 CCC=g++
else
 # Linux configuration
 CCFLAGS= -Wall
 CCC= g++
endif



# Absolute path of the MemoPlayer player src directory
MEMOPLAYER_DIR=~/Devel/workspace/MemoPlayer/src

# Path to the place where compiler should be in the SDK
SDK_DIR=~/Devel/workspace/WidgetSDK/tools/compiler
