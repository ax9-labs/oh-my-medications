package com.personal.medtracker;
import android.app.Activity;
import android.content.Context;

public abstract class LocalizedActivity extends Activity {
    private String attachedLanguage;
    @Override protected void attachBaseContext(Context base){attachedLanguage=AppLocale.tag(base);super.attachBaseContext(AppLocale.wrap(base));}
    @Override protected void onResume(){super.onResume();if(!attachedLanguage.equals(AppLocale.tag(this)))recreate();}
}
