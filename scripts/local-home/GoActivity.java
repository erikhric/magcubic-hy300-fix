package com.hy300.localhome;

import android.app.Activity;

/** Explicit am-start only. Do not put this on the launcher — v1 HIDL froze the box. */
public class GoActivity extends Activity {
    @Override
    protected void onCreate(android.os.Bundle b) {
        super.onCreate(b);
        finish();
    }
}
