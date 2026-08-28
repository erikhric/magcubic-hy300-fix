import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;

/** Switch Allwinner tvserver panel source. Image=LOCAL framebuffer; VideoDec=HDMI decoder (black if unplugged). */
public class SetSource {
    static final String IFACE = "vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer";
    static final String[] NAMES = {
        "Dummy", "VideoDec", "Image", "HDMI_1", "HDMI_2", "HDMI_3", "HDMI_4",
        "CVBS_1", "CVBS_2", "CVBS_3", "ATV", "DTV", "Max"
    };
    static final int TX_SET = 14;
    static final int TX_GET = 16;
    static final int TX_UNBLACK = 9;
    static final int IMAGE = 2;

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

    public static void main(String[] args) throws Exception {
        IHwBinder binder = HwBinder.getService(IFACE, "default");
        int cur = get(binder);
        System.out.println("current=" + cur + " (" + name(cur) + ")");
        if (args.length == 0) return;

        String cmd = args[0];
        if (cmd.equals("image") || cmd.equals("local")) {
            int rc = set(binder, IMAGE);
            System.out.println("set Image rc=" + rc);
            int ub = unblack(binder);
            System.out.println("unblack rc=" + ub);
        } else if (cmd.equals("set")) {
            int src = Integer.parseInt(args[1]);
            int rc = set(binder, src);
            System.out.println("set " + src + " (" + name(src) + ") rc=" + rc);
        } else if (cmd.equals("unblack")) {
            int ub = unblack(binder);
            System.out.println("unblack rc=" + ub);
        } else {
            System.err.println("usage: SetSource [image|set <id>|unblack]");
            System.exit(1);
        }
        int now = get(binder);
        System.out.println("now=" + now + " (" + name(now) + ")");
    }
}
