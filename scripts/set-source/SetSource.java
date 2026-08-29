import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;

/**
 * HIDL helper for vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer/default.
 *
 * TvSourceID: Dummy=0 LOCAL, VideoDec=1, Image=2, HDMI_1=3, HDMI_2=4, HDMI_3=5.
 * Dummy is LOCAL in HAL logs (SubDeviceSetSource → kHalSourceID_Dummy).
 * GetSource on a GOOD boot still returns VideoDec=1 — that is normal; the
 * picture is mixed on AV-MIPS, not an HDMI overlay we turned off.
 * Dummy SetSource / DeviceSvpStop / DeviceSvpStart hang when sys.svp_status=0
 * (MIPS stalled). Unplug ~10s; do not adb reboot. apply.sh never calls those.
 *
 * Transact: SvpStart=2, SvpStop=3, VpDeInit=5, Unblack=9, Uncover=11,
 * LoadConfig=12, SetSource=14, GetSource=16, HdmiPort=18.
 */
public class SetSource {
    static final String IFACE = "vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer";
    static final String[] NAMES = {
        "Dummy", "VideoDec", "Image", "HDMI_1", "HDMI_2", "HDMI_3", "HDMI_4",
        "CVBS_1", "CVBS_2", "CVBS_3", "ATV", "DTV", "Max"
    };
    static final int TX_SVPSTART = 2;
    static final int TX_SVPSTOP = 3;
    static final int TX_UNBLACK = 9;
    static final int TX_UNCOVER = 11;
    static final int TX_LOADCFG = 12;
    static final int TX_SET = 14;
    static final int TX_GET = 16;
    static final int TX_HDMI_PORT = 18;
    // SourceLoadConfig / portmap.cfg: TIF 1/2/3 → HDMI1/2/3, not TvSourceID.Image.
    // SetSource(2) is HDMI2 on this firmware, not LOCAL. Dummy=0 is LOCAL and hangs if MIPS is down.

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
        System.out.println("note: VideoDec on a good boot is normal; HDMI overlay is not disabled.");
        System.out.println("note: svp_status=0 → unplug 10s. do not set dummy / svpstop.");
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

    static void showAndroid(IHwBinder binder) throws Exception {
        // Unblack + Uncover only. Does not switch the mux and does not disable HDMI.
        // Do not DeviceSvpStop (transact 3): hangs tvserver if AV-MIPS is stalled.
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
        if (cmd.equals("dump") || cmd.equals("get")) {
            if (cmd.equals("dump")) dump(binder);
            return;
        } else if (cmd.equals("image") || cmd.equals("local")) {
            showAndroid(binder);
        } else if (cmd.equals("set")) {
            int src = Integer.parseInt(args[1]);
            if (src == 0) {
                System.err.println("warn: Dummy=0 is LOCAL; RPC hangs if sys.svp_status=0. Unplug, do not adb reboot.");
            }
            int rc = set(binder, src);
            System.out.println("set " + src + " (" + name(src) + ") rc=" + rc);
        } else if (cmd.equals("unblack")) {
            System.out.println("unblack rc=" + unblack(binder));
        } else if (cmd.equals("uncover")) {
            System.out.println("uncover0 rc=" + uncover(binder, 0));
            System.out.println("uncover1 rc=" + uncover(binder, 1));
        } else if (cmd.equals("svpstop")) {
            System.err.println("warn: DeviceSvpStop hangs tvserver if sys.svp_status=0.");
            System.out.println("svpstop rc=" + tx(binder, TX_SVPSTOP));
        } else if (cmd.equals("svpstart")) {
            System.err.println("warn: DeviceSvpStart hangs tvserver if sys.svp_status=0.");
            System.out.println("svpstart rc=" + tx(binder, TX_SVPSTART));
        } else {
            System.err.println("usage: SetSource dump | get | image | local | set <id> | unblack | uncover | svpstop | svpstart");
            System.err.println("  dump/get are safe. set 0 / svpstop hang if sys.svp_status=0. apply.sh never calls those.");
            System.exit(1);
        }
        int now = get(binder);
        System.out.println("now=" + now + " (" + name(now) + ")");
    }
}
