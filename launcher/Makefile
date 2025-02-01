
LOADER = loader
LOADER_OBJS = loader.o

CXX = gcc
XXD = xxd
STRIP = strip
WINDRES = windres
RM = rm

CXXFLAGS = -O2 -Wall -fmessage-length=0 -I "$(JAVA_HOME)/include"

ifeq ($(OS),Windows_NT)
	CXXFLAGS += -I "$(JAVA_HOME)/include/win32"
	LOADER_OBJS += loader.res
	LIBS = -mwindows -static-libgcc -Wl,-static -ldl
	EXT = .exe
else
	UNAME_S := $(shell uname -s)
	UNAME_P := $(shell uname -p)
	ifeq ($(UNAME_S),Linux)
		CXXFLAGS += -I "$(JAVA_HOME)/include/linux"
		LOADER_OBJS += loader32.o loader48.o loader64.o
	endif
	ifeq ($(UNAME_S),Darwin)
		ifeq ($(UNAME_P),x86_64)
			CXXFLAGS += -mmacosx-version-min=10.15
		else
			CXXFLAGS += -mmacosx-version-min=11.0
		endif
		CXXFLAGS += -I "$(JAVA_HOME)/include/darwin"
	endif
	LIBS =
	EXT =
endif

all: loader$(EXT)

loader$(EXT): Makefile $(LOADER_OBJS)
	$(CXX) -o $@ $(LOADER_OBJS) $(LIBS)
	$(STRIP) $@

%.o: %.c
	$(CXX) $(CXXFLAGS) -o $@ -c $<

%.res: %.rc
	$(WINDRES) -O coff -o $@ -i $<

%.c : %.png
	$(XXD) -i $< $@

clean:
	$(RM) -f loader$(EXT) $(LOADER_OBJS)
