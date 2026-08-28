package com.hy300.localhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;

/** After boot: panel → Image (Android), then HOME (Projectivy). */
public class BootReceiver extends BroadcastReceiver {
    static final String IFACE = "vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        go(ctx);
    }

    static void go(Context ctx) {
        try {
            IHwBinder binder = HwBinder.getService(IFACE, "default");
            set(binder, 2);
            unblack(binder);
        } catch (Throwable ignored) {
            // ponytail: HIDL may be hidden-API blocked for this uid; oem shell script is the real switch
        }
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(home);
        } catch (Throwable ignored) {
        }
    }

    static int set(IHwBinder binder, int src) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        req.writeInt32(src);
        binder.transact(14, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readInt32();
        } finally {
            resp.release();
        }
    }

    static int unblack(IHwBinder binder) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        binder.transact(9, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readInt32();
        } finally {
            resp.release();
        }
    }

    public static class GoActivity extends Activity {
        @Override
        protected void onCreate(android.os.Bundle b) {
            super.onCreate(b);
            BootReceiver.go(this);
            finish();
        }
    }
}
