package org.seh;

import com.schneiderautomation.sysdiag.SysDiagStrings;
import java.util.Locale;

class EnetStrings
{
  private static final String bundleName = "EnetStrBundle";
  private static SysDiagStrings strings;
  
  static void init(Locale paramLocale)
  {
    strings = new SysDiagStrings("EnetStrBundle", paramLocale);
  }
  
  static String getString(String paramString)
  {
    return strings != null ? strings.getString(paramString) : "<string resources not loaded>";
  }
  
  static String getFormattedString(String paramString, Object[] paramArrayOfObject)
  {
    return strings != null ? strings.getFormattedString(paramString, paramArrayOfObject) : "<string resources not loaded>";
  }
}
