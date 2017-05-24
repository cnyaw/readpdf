
LOCAL_PATH:= $(call my-dir)

XPDF_DIR        := ../../xpdf

FOFI_SRC_DIR    := $(XPDF_DIR)/fofi
FOFI_SRC_0      := FoFiBase.cc FoFiEncodings.cc FoFiIdentifier.cc FoFiTrueType.cc \
                   FoFiType1.cc FoFiType1C.cc
FOFI_SRC        := $(addprefix $(FOFI_SRC_DIR)/, $(FOFI_SRC_0))

GOO_SRC_DIR     := $(XPDF_DIR)/goo
GOO_SRC_0       := gfile.cc GHash.cc GList.cc gmem.cc gmempp.cc GString.cc parseargs.c
GOO_SRC         := $(addprefix $(GOO_SRC_DIR)/, $(GOO_SRC_0))

XPDF_SRC_DIR    := $(XPDF_DIR)/xpdf
XPDF_SRC_0      := AcroForm.cc Annot.cc Array.cc BuiltinFont.cc BuiltinFontTables.cc \
                   Catalog.cc CharCodeToUnicode.cc CMap.cc Decrypt.cc Dict.cc \
                   Error.cc FontEncodingTables.cc Form.cc Function.cc Gfx.cc \
                   GfxFont.cc GfxState.cc GlobalParams.cc JArithmeticDecoder.cc \
                   JBIG2Stream.cc JPXStream.cc Lexer.cc Link.cc NameToCharCode.cc \
                   Object.cc OptionalContent.cc Outline.cc OutputDev.cc Page.cc \
                   Parser.cc PDFDoc.cc PDFDocEncoding.cc PSTokenizer.cc SecurityHandler.cc \
                   Stream.cc TextOutputDev.cc TextString.cc UnicodeMap.cc \
                   UnicodeTypeTable.cc XFAForm.cc XRef.cc Zoox.cc
XPDF_SRC        := $(addprefix $(XPDF_SRC_DIR)/, $(XPDF_SRC_0)) readpdf.cpp

SPLASH_SRC_DIR  := $(XPDF_DIR)/splash

include $(CLEAR_VARS)

LOCAL_MODULE    := libreadpdf
LOCAL_CFLAGS    := -D_linux_ -DLUA_ANSI -D_android_ -fexceptions
LOCAL_SRC_FILES := $(FOFI_SRC) $(GOO_SRC) $(XPDF_SRC)
                   
LOCAL_LDLIBS    := -llog -lz -landroid

LOCAL_C_INCLUDES := $(XPDF_DIR) $(XPDF_SRC_DIR) $(FOFI_SRC_DIR) $(GOO_SRC_DIR) $(SPLASH_SRC_DIR)

include $(BUILD_SHARED_LIBRARY)
