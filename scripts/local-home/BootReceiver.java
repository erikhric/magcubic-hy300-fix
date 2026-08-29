package com.hy300.localhome;

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
            // ponytail: SetSource(2) is HDMI2 on this firmware; drop the video plane instead
            tx(binder, 5);
            unblack(binder);
            uncover(binder, 0);
            uncover(binder, 1);
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

    static int tx(IHwBinder binder, int code) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        binder.transact(code, req, resp, 0);
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

    static int uncover(IHwBinder binder, int id) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        req.writeInt32(id);
        binder.transact(11, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readInt32();
        } finally {
            resp.release();
        }
    }
}
