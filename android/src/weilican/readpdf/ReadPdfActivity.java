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

  boolean isDocView;
  File lstFile[];
  static ProgressDialog dlgProgress;
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

    if (!isTaskRoot()) {
      finish();
      return;
    }

    setAppTheme();
    setDocListContent();
    Intent intent = getIntent();
    if (null != intent) {
      String type = intent.getType();
      if (null != type && type.equals("application/pdf")) {
        Uri uri = intent.getData();
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
      if (System.currentTimeMillis() < back_pressed + 2000) {
        setDocListContent();
      } else {
        Toast.makeText(this, "Press once again to exit!", Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
      }
    } else {
      finish();
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    menu.clear();
    if (isDocView) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.item_menu, menu);
    }
    super.onCreateContextMenu(menu, v, menuInfo);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
    switch (item.getItemId())
    {
    case R.id.item_open:
      {
        Intent newIntent = new Intent(Intent.ACTION_VIEW);
        newIntent.setDataAndType(Uri.fromFile(lstFile[info.position]), "text/plain");
        try {
          startActivity(newIntent);
        } catch (ActivityNotFoundException e) {
          Toast.makeText(this, "No handler for this type of file.", Toast.LENGTH_LONG).show();
        }
      }
      return true;
    case R.id.item_delete:
      lstFile[info.position].delete();
      setDocListContent();
      return true;
    }
    return super.onContextItemSelected(item);
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
    File dirCache = new File(getDiskCacheDir(this));
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
        setTextViewContent(lstFile[index].toString());
      }
    });
    Button btn = (Button)findViewById(R.id.btn_open);
    btn.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        chooseFile();
      }
    });
    registerForContextMenu(listView);
    isDocView = true;
  }

  void chooseFile() {
    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
    chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
    chooseFile.setType("application/pdf");
    Intent intent = Intent.createChooser(chooseFile, "Choose a pdf file");
    startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
  }

  String replaceFileExt(String path, String ext) {
    return path.substring(0, path.lastIndexOf('.')) + "." + ext;
  }

  void setTextViewContent(String txtPath) {
    setContentView(R.layout.main);
    TextView capTxt = (TextView)findViewById(R.id.captxt);
    capTxt.setText(txtPath);
    final TextView pdfTxt = (TextView)findViewById(R.id.pdftxt);
    setFontSize();
    txtString = readText(txtPath);
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
    File file = new File(txtPath);
    if (!file.exists()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      while (null != (line = br.readLine())) {
        text.append(line);
        text.append('\n');
      }
    } catch (IOException e) {
      Toast.makeText(this, "Read " + txtPath + " fail!", Toast.LENGTH_LONG).show();
    }
    return text.toString();
  }

  String getDiskCacheDir(Context context) {
    String cachePath = null;
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
       !Environment.isExternalStorageRemovable()) {
      cachePath = context.getExternalCacheDir().getPath();
    } else {
      cachePath = context.getCacheDir().getPath();
    }
    return cachePath;
  }

  boolean doConvertPdf(final String pdfPath, final String txtPath) {
    final Handler h = new Handler();
    dlgProgress = ProgressDialog.show(this, "Converting pdf to txt", "Please wait",true);
    new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              pdftotxt(pdfPath, txtPath);
            } finally {
              dlgProgress.dismiss();
              dlgProgress = null;
              h.post(new Runnable() {
                @Override
                public void run() {
                  setTextViewContent(txtPath);
                }
              });
            }
          }
     }).start();
    return true;
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

  boolean showWarnDlg(final String pdfPath, final String txtPath) {
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
        if (doConvertPdf(pdfPath, txtPath) && null == dlgProgress) {
          setTextViewContent(txtPath);
        }
      }
    });
    alert.show();
    return true;
  }

  void openPdf(Uri uri) {
    String pdfPath = uri.getPath();
    if (!pdfPath.toLowerCase().contains(".pdf")) {
      Toast.makeText(this, "Convert " + pdfPath + " to pdf fail!", Toast.LENGTH_LONG).show();
      return;
    }
    String tmpPath = getDiskCacheDir(this) + "/" + uri.getLastPathSegment();
    String txtPath = replaceFileExt(tmpPath, "txt");
    File file = new File(txtPath);
    if (file.exists()) {
      setTextViewContent(txtPath);
      return;
    }
    if (!showWarnDlg(pdfPath, txtPath)) {
      if (doConvertPdf(pdfPath, txtPath) && null == dlgProgress) {
        setTextViewContent(txtPath);
      }
    }
  }

  //
  // JNI.
  //

  static native boolean pdftotxt(String pdfPath, String txtPath);

  static {
    System.loadLibrary("readpdf");
  }
}
