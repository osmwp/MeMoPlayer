include ../Makefile.inc

ifeq ($(TARGET), macos)
FE_CCFLAGS=-Wall -Imacos/FreeImage/Dist  -Imacos/FreeType/include
FE_LDFLAGS= -Wl,-framework,CoreServices,-framework,ApplicationServices -Lmacos/FreeImage/Dist -lfreeimage -Lmacos/FreeType/objs -lfreetype
else ifeq ($(TARGET), cygwin)
FE_CCFLAGS=-Wall -Icygwin/FreeImage/Dist  -I/usr/include/freetype2 -D_WIN32
FE_LDFLAGS= -lfreetype -Lcygwin/FreeImage/Dist/ -lfreeimage
else ifeq ($(TARGET), linux)
FE_CCFLAGS=-Wall -Ilinux/FreeImage/Dist  -Ilinux/FreeType/include
FE_LDFLAGS=-Llinux/FreeImage/Dist -lfreeimage -Llinux/FreeType/objs -lfreetype
else
MSG=export TARGET with either macos, linux or cygwin
CCC=echo ${MSG}
endif 

../fontExtractor: fontExtractor.cpp
	$(CCC) $(CCFLAGS) $(FE_CCFLAGS) -o ../fontExtractor fontExtractor.cpp $(FE_LDFLAGS)
