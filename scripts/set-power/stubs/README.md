Compile-only stubs so `SetPower.java` javac's against `android.os.HwBinder` / `HwParcel`. They are **not** packaged into the dex; the device boot classpath provides the real classes.
