/*
  readpdf.cpp
  Read PDF.

  Copyright (c) 2016 Waync Cheng.
  All Rights Reserved.

  2016/5/27 Waync created
 */

#include <jni.h>
#include <android/log.h>

#include <aconf.h>
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <string.h>

#include <string>

#include "GlobalParams.h"
#include "PDFDoc.h"
#include "TextOutputDev.h"
#include "UnicodeMap.h"
#include "Error.h"

extern "C" {

void pdftotxtTextOutputFunc(void *stream, const char *text, int len)
{
  std::string *pStr = (std::string*)stream;
  pStr->append(text, len);
}

JNIEXPORT jbyteArray JNICALL Java_weilican_readpdf_ReadPdfActivity_pdftotxt(JNIEnv * env, jobject obj, jbyteArray pdfdata)
{
  if (!pdfdata) {
    return NULL;
  }

  jbyte *pBytes = env->GetByteArrayElements(pdfdata, NULL);
  int lenBytes = env->GetArrayLength(pdfdata);

  int firstPage = 1;
  int lastPage = 0;
  double fixedPitch = 0;
  GBool clipText = gFalse;
  char textEncName[128] = "UTF-8";
  char textEOL[16] = "";
  GBool noPageBreaks = gFalse;
  char ownerPassword[33] = "\001";
  char userPassword[33] = "\001";
  char cfgFileName[256] = "";

  PDFDoc *doc;
  MemStream *memstr;
  GString *ownerPW, *userPW;
  TextOutputControl textOutControl;
  TextOutputDev *textOut;
  UnicodeMap *uMap;

  jbyteArray pRetBytes = NULL;
  std::string outstr;

  Object nulObj;
  nulObj.initNull();
  memstr = new MemStream((char*)pBytes, 0, (Guint)lenBytes, &nulObj);

  // read config file
  globalParams = new GlobalParams(cfgFileName);
  if (textEncName[0]) {
    globalParams->setTextEncoding(textEncName);
  }
  if (textEOL[0]) {
    if (!globalParams->setTextEOL(textEOL)) {
      fprintf(stderr, "Bad '-eol' value on command line\n");
    }
  }
  if (noPageBreaks) {
    globalParams->setTextPageBreaks(gFalse);
  }

  // get mapping to output encoding
  if (!(uMap = globalParams->getTextEncoding())) {
    error(errConfig, -1, "Couldn't get text encoding");
    delete memstr;
    goto err1;
  }

  // open PDF file
  if (ownerPassword[0] != '\001') {
    ownerPW = new GString(ownerPassword);
  } else {
    ownerPW = NULL;
  }
  if (userPassword[0] != '\001') {
    userPW = new GString(userPassword);
  } else {
    userPW = NULL;
  }
  doc = new PDFDoc(memstr, ownerPW, userPW);
  if (userPW) {
    delete userPW;
  }
  if (ownerPW) {
    delete ownerPW;
  }
  if (!doc->isOk()) {
    goto err2;
  }

  // check for copy permission
  if (!doc->okToCopy()) {
    error(errNotAllowed, -1, "Copying of text from this document is not allowed.");
    goto err2;
  }

  // get page range
  if (firstPage < 1) {
    firstPage = 1;
  }
  if (lastPage < 1 || lastPage > doc->getNumPages()) {
    lastPage = doc->getNumPages();
  }

  // write text file
  textOutControl.mode = textOutPhysLayout;
  textOutControl.fixedPitch = fixedPitch;
  textOutControl.clipText = clipText;

  textOut = new TextOutputDev(pdftotxtTextOutputFunc, &outstr, &textOutControl);
  if (textOut->isOk()) {
    doc->displayPages(textOut, firstPage, lastPage, 72, 72, 0, gFalse, gTrue, gFalse);
  } else {
    delete textOut;
    goto err2;
  }
  delete textOut;

  pRetBytes = env->NewByteArray(outstr.length());
  env->SetByteArrayRegion(pRetBytes, 0, outstr.length(), (const jbyte*)outstr.c_str());

  // clean up
err2:
  delete doc;
  uMap->decRefCnt();
err1:
  delete globalParams;
err0:
  env->ReleaseByteArrayElements(pdfdata, pBytes, JNI_ABORT);

  return pRetBytes;
}

};
