import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;

/** factorySetPowerMode: 0=STANDBY 1=DIRECT (boot on plug) 2=MEMORY. Not the HDMI/LOCAL mux. */
public class SetPower {
    static final String IFACE = "vendor.aw.homlet.tvsystem.tvserver@1.0::ITvServer";
    static final String[] MODES = {"STANDBY", "DIRECT", "MEMORY"};

    static String name(int m) {
        return (m >= 0 && m < MODES.length) ? MODES[m] : ("unknown:" + m);
    }

    static int get(IHwBinder binder) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        binder.transact(153, req, resp, 0);
        resp.verifySuccess();
        req.releaseTemporaryStorage();
        try {
            return resp.readInt32();
        } finally {
            resp.release();
        }
    }

    static int set(IHwBinder binder, int mode) throws Exception {
        HwParcel req = new HwParcel();
        HwParcel resp = new HwParcel();
        req.writeInterfaceToken(IFACE);
        req.writeInt32(mode);
        binder.transact(152, req, resp, 0);
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
        int mode = Integer.parseInt(args[0]);
        int rc = set(binder, mode);
        System.out.println("set rc=" + rc);
        int now = get(binder);
        System.out.println("now=" + now + " (" + name(now) + ")");
    }
}
