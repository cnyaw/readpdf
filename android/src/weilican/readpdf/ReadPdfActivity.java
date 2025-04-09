/*
  ReadPdfActivity.java
  Read PDF.

  Copyright (c) 2016 Waync Cheng.
  All Rights Reserved.

  2016/5/27 Waync created
 */

package weilican.readpdf;

import android.os.Bundle;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.view.ContextMenu.ContextMenuInfo;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.widget.AdapterView.*;
import android.os.*;
import android.app.*;
import android.content.*;
import android.widget.*;
import android.view.*;
import java.io.*;

public class ReadPdfActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener
{
  static final int ACTIVITY_CHOOSE_FILE = 1;
  static final int LOAD_MORE_SIZE = 16 * 1024;
  static final int BACK_PRESS_AGAIN_TO_EXIT_TIME = 2000;

  static ReadPdfActivity thisActivity;
  boolean isDocView;
  File lstFile[];
  String txtString;
  int curPos, endPos;
  long back_pressed;

  //
  // Override.
  //

  @Override
  protected void onCreate(Bundle b)
  {
    super.onCreate(b);

    thisActivity = this;
    setAppTheme();
    setDocListContent();
    Intent intent = getIntent();
    if (null != intent) {
      String type = intent.getType();
      if (null != type && type.equals("application/pdf")) {
        String action = intent.getAction();
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(action)) {
          uri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(action)) {
          uri = (Uri)intent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        if (null != uri) {
          openPdf(uri);
        }
      }
    }
    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.settings, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId())
    {
    case R.id.menu_settings:
      Intent i = new Intent(this, SettingsActivity.class);
      startActivity(i);
      break;
    }
    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (ACTIVITY_CHOOSE_FILE == requestCode && RESULT_OK == resultCode) {
      openPdf(data.getData());
    }
  }

  @Override
  public void onBackPressed() {
    if (!isDocView) {
      if (System.currentTimeMillis() < back_pressed + BACK_PRESS_AGAIN_TO_EXIT_TIME) {
        setDocListContent();
      } else {
        Toast.makeText(this, "Press once again to exit!", Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
      }
    } else {
      finish();
    }
  }

  void showDocItemPopupMenu(View view, final int position) {
    PopupMenu popupMenu = new PopupMenu(this, view);
    popupMenu.getMenuInflater().inflate(R.menu.item_menu, popupMenu.getMenu());
    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
          case R.id.item_open:
            {
              Intent newIntent = new Intent(Intent.ACTION_VIEW);
              newIntent.setDataAndType(Uri.fromFile(lstFile[position]), "text/plain");
              try {
                startActivity(newIntent);
              } catch (ActivityNotFoundException e) {
                Toast.makeText(thisActivity, "No handler for this type of file.", Toast.LENGTH_LONG).show();
              }
            }
            return true;
          case R.id.item_delete:
            lstFile[position].delete();
            setDocListContent();
            return true;
          default:
            return false;
        }
      }
    });
    popupMenu.show();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    setFontSize();
  }

  //
  // Implementation.
  //

  void setAppTheme() {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    boolean isLightTheme = sharedPrefs.getBoolean("prefLightTheme", true);
    if (isLightTheme) {
      setTheme(R.style.AppThemeLight);
    } else {
      setTheme(R.style.AppThemeBlack);
    }
  }

  void setFontSize() {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    int fontSize = Integer.parseInt(sharedPrefs.getString("prefFontSize", "16842804"));
    switch (fontSize)
    {
    case 1:
      fontSize = android.R.style.TextAppearance_Small;
      break;
    case 2:
      fontSize = android.R.style.TextAppearance;
      break;
    case 3:
      fontSize = android.R.style.TextAppearance_Medium;
      break;
    case 4:
      fontSize = android.R.style.TextAppearance_Large;
      break;
    }
    TextView pdfTxt = (TextView)findViewById(R.id.pdftxt);
    if (null != pdfTxt) {
      pdfTxt.setTextAppearance(this, fontSize);
    }
  }

  void setDocListContent() {
    setContentView(R.layout.doclist);
    File dirCache = new File(getTempPath());
    lstFile = dirCache.listFiles();
    String values[] = new String[lstFile.length];
    for (int i = 0; i < lstFile.length; i++) {
      values[i] = lstFile[i].getName();
    }
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, values);
    ListView listView = (ListView)findViewById(R.id.list_view);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        setTextViewContent(lstFile[index].toString(), null);
      }
    });
    listView.setOnItemLongClickListener(new OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
         showDocItemPopupMenu(arg1, arg2);
         return true;
      }
    });
    Button btn = (Button)findViewById(R.id.btn_open);
    btn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        chooseFile();
      }
    });
    isDocView = true;
  }

  void chooseFile() {
    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
    chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
    chooseFile.setType("application/pdf");
    Intent intent = Intent.createChooser(chooseFile, "Choose a PDF file");
    startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
  }

  void setTextViewContent(String txtPath, byte bytes[]) {
    setContentView(R.layout.main);
    TextView capTxt = (TextView)findViewById(R.id.captxt);
    capTxt.setText(txtPath);
    capTxt.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        String text = ((TextView)v).getText().toString();
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), "Text path has copied to clipboard.", Toast.LENGTH_LONG).show();
      }
    });
    final TextView pdfTxt = (TextView)findViewById(R.id.pdftxt);
    setFontSize();
    if (null != bytes) {
      try {
        txtString = new String(bytes, "UTF-8");
      } catch (Exception e) {
        txtString = readText(txtPath);
      }
    } else {
      txtString = readText(txtPath);
    }
    curPos = 0;
    endPos = getEndPos();
    pdfTxt.setText(txtString.substring(0, endPos));
    curPos = endPos;
    final Button btnMore = (Button)findViewById(R.id.load_more);
    if (txtString.length() == endPos) {
      removeLoadMoreBtn(btnMore);
    } else {
      btnMore.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          endPos = getEndPos();
          pdfTxt.append(txtString.substring(curPos, endPos));
          curPos = endPos;
          if (txtString.length() == endPos) {
            removeLoadMoreBtn(btnMore);
          }
        }
      });
    }
    isDocView = false;
  }

  int getEndPos() {
    endPos = curPos + LOAD_MORE_SIZE;
    if (txtString.length() < endPos) {
      endPos = txtString.length();
    }
    return endPos;
  }

  void removeLoadMoreBtn(Button btnMore) {
    ViewGroup layout = (ViewGroup)btnMore.getParent();
    if (null != layout) {
      layout.removeView(btnMore);
    }
  }

  String readText(String txtPath) {
    try {
      File file = new File(txtPath);
      if (!file.exists()) {
        return "";
      }
      StringBuilder text = new StringBuilder();
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      while (null != (line = br.readLine())) {
        text.append(line);
        text.append('\n');
      }
      return text.toString();
    } catch (Exception e) {
      Toast.makeText(this, "Read " + txtPath + " fail!", Toast.LENGTH_LONG).show();
    }
    return "";
  }

  void doConvertPdf(final byte pdfdata[], final String txtPath) {
    final ProgressDialog dlgProgress = ProgressDialog.show(this, "Converting PDF to TXT", "Please wait",true);
    new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              final byte txtdata[] = pdftotxt(pdfdata);
              dlgProgress.dismiss();
              if (null != txtdata && 0 < txtdata.length) {
                FileOutputStream os = new FileOutputStream(txtPath);
                os.write(txtdata);
                os.close();
                runOnUiThread(new Thread(new Runnable() {
                  public void run() {
                    setTextViewContent(txtPath, txtdata);
                  }
                }));
              } else {
                runOnUiThread(new Thread(new Runnable() {
                  public void run() {
                    Toast.makeText(thisActivity, "Convert PDF to TXT fail!", Toast.LENGTH_LONG).show();
                  }
                }));
              }
            } catch (final Exception e) {
              dlgProgress.dismiss();
              runOnUiThread(new Thread(new Runnable() {
                public void run() {
                  Toast.makeText(thisActivity, "Convert PDF to TXT fail! " + e.toString(), Toast.LENGTH_LONG).show();
                }
              }));
            }
          }
     }).start();
  }

  boolean isPrefConvertWarn() {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    return sharedPrefs.getBoolean("prefConvertWarn", false);
  }

  void savePrefConvertWarn(boolean b) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = sharedPrefs.edit();
    editor.putBoolean("prefConvertWarn", b);
    editor.commit();
  }

  boolean showWarnDlg(final byte pdfdata[], final String txtPath) {
    if (!isPrefConvertWarn()) {
      return false;
    }
    AlertDialog.Builder alert = new AlertDialog.Builder(this);
    LayoutInflater inflater = LayoutInflater.from(this);
    View layout = inflater.inflate(R.layout.warndlg, null);
    final CheckBox dontShowAgain = (CheckBox)layout.findViewById(R.id.warn_dont_show_again);
    alert.setView(layout);
    alert.setMessage(R.string.warn_msg);
    alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        savePrefConvertWarn(!dontShowAgain.isChecked());
        doConvertPdf(pdfdata, txtPath);
      }
    });
    alert.show();
    return true;
  }

  String getTempPath() {
    String cachePath = null;
    File extCacheDir = Environment.getExternalStorageDirectory();
    if (null != extCacheDir) {
      cachePath = extCacheDir.getPath();
    } else {
      cachePath = getCacheDir().getPath();
    }
    String tmpDir = cachePath + "/temp/";
    File dir = new File(tmpDir);
    if (!dir.exists()) {
      dir.mkdirs();                     // Create the directory if it doesn't exist
    }
    return tmpDir;
  }

  String getPdfTmpPath(Uri uri) {
    String tmpDir = getTempPath();
    String pdfPath = uri.getPath();
    String filename = pdfPath.substring(pdfPath.lastIndexOf("/") + 1);
    return tmpDir + filename + ".txt";
  }

  byte[] bytesFromUri(Uri uri) {
    try {
      InputStream is = getContentResolver().openInputStream(uri);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      int nRead;
      byte data[] = new byte[16384];
      while (-1 != (nRead = is.read(data, 0, data.length))) {
        os.write(data, 0, nRead);
      }
      os.flush();
      return os.toByteArray();
    } catch (Exception e) {
      return null;
    }
  }

  void openPdf(Uri uri) {
    String txtPath = getPdfTmpPath(uri);
    File file = new File(txtPath);
    if (file.exists()) {
      setTextViewContent(txtPath, null);
      return;
    }
    byte pdfdata[] = bytesFromUri(uri);
    if (null == pdfdata) {
      Toast.makeText(this, "Read PDF data fail!", Toast.LENGTH_LONG).show();
      return;
    }
    if (!showWarnDlg(pdfdata, txtPath)) {
      doConvertPdf(pdfdata, txtPath);
    }
  }

  //
  // JNI.
  //

  static native byte[] pdftotxt(byte pdfdata[]);

  static {
    System.loadLibrary("readpdf");
  }
}
