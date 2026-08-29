package com.hy300.localhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** After boot: send HOME (Projectivy). Does not poke tvserver — HIDL setters hang AV-MIPS when stalled. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        go(ctx);
    }

    static void go(Context ctx) {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(home);
        } catch (Throwable ignored) {
        }
    }
}
