package com.hy300.localhome;

import android.app.Activity;

/** First-launch activity so Android 8+ will deliver BOOT_COMPLETED. */
public class GoActivity extends Activity {
    @Override
    protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        BootReceiver.go(this);
        finish();
    }
}
