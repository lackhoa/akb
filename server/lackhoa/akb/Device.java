package lackhoa.akb;

import lackhoa.akb.wrappers.InputManager;
import lackhoa.akb.wrappers.ServiceManager;

import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Device {

    private static final ServiceManager SERVICE_MANAGER = new ServiceManager();

    public static boolean injectEvent(InputEvent inputEvent) {
        return SERVICE_MANAGER
            .getInputManager()
            .injectInputEvent(inputEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
    }
}
