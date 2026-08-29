import android.os.HwBinder;
import android.os.HwBlob;
import android.os.HwParcel;
import android.os.IHwBinder;

/** Switch Allwinner tvserver panel source. Image=LOCAL framebuffer; VideoDec=HDMI decoder (black if unplugged). */
public class SetSource {
    static final String IFACE = "vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer";
    static final String[] NAMES = {
        "Dummy", "VideoDec", "Image", "HDMI_1", "HDMI_2", "HDMI_3", "HDMI_4",
        "CVBS_1", "CVBS_2", "CVBS_3", "ATV", "DTV", "Max"
    };
    static final int TX_SVPSTART = 2;
    static final int TX_SVPSTOP = 3;
    static final int TX_VPINIT = 4;
    static final int TX_VPDEINIT = 5;
    static final int TX_UNBLACK = 9;
    static final int TX_UNCOVER = 11;
    static final int TX_LOADCFG = 12;
    static final int TX_SET = 14;
    static final int TX_GET = 16;
    static final int TX_HDMI_PORT = 18;
    static final int TX_VPWINDOW = 20;
    // This firmware's SourceLoadConfig maps 1=HDMI1, 2=HDMI2 — not TvSourceID.Image.
    // LOCAL is the Android plane: tear down the HDMI video plane, do not SetSource(2).

    static String name(int id) {
        return (id >= 0 && id < NAMES.length) ? NAMES[id] : ("id:" + id);
    }

    static int get(IHwBinder binder) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        binder.transact(TX_GET, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readInt32();
        } finally {
            resp.release();
        }
    }

    static int set(IHwBinder binder, int src) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        req.writeInt32(src);
        binder.transact(TX_SET, req, resp, 0);
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
        binder.transact(TX_UNBLACK, req, resp, 0);
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
        binder.transact(TX_UNCOVER, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readInt32();
        } finally {
            resp.release();
        }
    }

    static String loadCfg(IHwBinder binder) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        binder.transact(TX_LOADCFG, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readString();
        } finally {
            resp.release();
        }
    }

    static int hdmiPort(IHwBinder binder) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        binder.transact(TX_HDMI_PORT, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readInt32();
        } finally {
            resp.release();
        }
    }

    static void dump(IHwBinder binder) throws Exception {
        System.out.println("cfg=" + loadCfg(binder));
        System.out.println("hdmiPort=" + hdmiPort(binder));
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

    static void writeWin(HwParcel req, int hs, int hz, int vs, int vz) {
        HwBlob blob = new HwBlob(16);
        blob.putInt32(0, hs);
        blob.putInt32(4, hz);
        blob.putInt32(8, vs);
        blob.putInt32(12, vz);
        req.writeBuffer(blob);
    }

    static int setVpWindow(IHwBinder binder) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        writeWin(req, 0, 0, 0, 0);
        writeWin(req, 0, 0, 0, 0);
        req.writeInt32(0);
        binder.transact(TX_VPWINDOW, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readInt32();
        } finally {
            resp.release();
        }
    }

    static void showAndroid(IHwBinder binder) throws Exception {
        // Do not DeviceSvpStop (transact 3): it hangs tvserver/app_process.
        // Do not SetSource(2): this firmware maps 2 to HDMI2.
        System.out.println("unblack rc=" + unblack(binder));
        System.out.println("uncover0 rc=" + uncover(binder, 0));
        System.out.println("uncover1 rc=" + uncover(binder, 1));
    }

    public static void main(String[] args) throws Exception {
        IHwBinder binder = HwBinder.getService(IFACE, "default");
        int cur = get(binder);
        System.out.println("current=" + cur + " (" + name(cur) + ")");
        if (args.length == 0) {
            dump(binder);
            return;
        }

        String cmd = args[0];
        if (cmd.equals("image") || cmd.equals("local")) {
            showAndroid(binder);
        } else if (cmd.equals("set")) {
            int src = Integer.parseInt(args[1]);
            int rc = set(binder, src);
            System.out.println("set " + src + " (" + name(src) + ") rc=" + rc);
        } else if (cmd.equals("unblack")) {
            System.out.println("unblack rc=" + unblack(binder));
        } else if (cmd.equals("uncover")) {
            System.out.println("uncover0 rc=" + uncover(binder, 0));
            System.out.println("uncover1 rc=" + uncover(binder, 1));
        } else if (cmd.equals("svpstop")) {
            System.out.println("svpstop rc=" + tx(binder, TX_SVPSTOP));
        } else if (cmd.equals("svpstart")) {
            System.out.println("svpstart rc=" + tx(binder, TX_SVPSTART));
        } else if (cmd.equals("dump")) {
            dump(binder);
            return;
        } else {
            System.err.println("usage: SetSource [image|set <id>|unblack|uncover|vpinit|dump]");
            System.exit(1);
        }
        int now = get(binder);
        System.out.println("now=" + now + " (" + name(now) + ")");
    }
}
