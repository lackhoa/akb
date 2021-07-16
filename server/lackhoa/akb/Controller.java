package lackhoa.akb;

import android.os.Build;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// The most "sophisticated" class: it receives messages,
// and inject the correct events (which is surprisingly not trivial, and involves state)
public class Controller {

    private static final int DEFAULT_DEVICE_ID = 0;

    private long downTime = 0;     // Time since last ACTION_DOWN
    private int activePointerCount = 0;  // Active pointers within the MAX_POINTERS range

    private final Device device;

    private final MotionEvent.PointerProperties[] pointerProperties =
        new MotionEvent.PointerProperties[Const.MAX_POINTERS];
    private final MotionEvent.PointerCoords[] pointerCoords =
        new MotionEvent.PointerCoords[Const.MAX_POINTERS];

    public Controller(Device device) {
        this.device = device;
        initPointers();
    }

    private void initPointers() {
        for (int i = 0; i < Const.MAX_POINTERS; i++) {
            MotionEvent.PointerProperties props = new MotionEvent.PointerProperties();
            props.toolType = MotionEvent.TOOL_TYPE_FINGER;
            pointerProperties[i] = props;

            MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
            coords.orientation = 0;
            coords.size = 0;
            pointerCoords[i] = coords;
        }
    }

    // keyEvent stands for an sdl key event
    // #sleep Coordinates shouldn't be ints but floats
    // Returns true if message can be returned
    public boolean handleMessage(int keyEvent, int x, int y) {
        int action;
        boolean isKeyUp;

        long now = SystemClock.uptimeMillis();
        switch(keyEvent) {
        case Const.SDL_KEYDOWN:
            isKeyUp = false;
            break;
        case Const.SDL_KEYUP:
            isKeyUp = true;
            break;
        default:
            Ln.e("Can't handle sdl event: " + keyEvent);
            return false;
        }

        // Update or add pointer
        int ptrIndex = 0;
        if (isKeyUp) {
            boolean found = false;
            for (int i = 0; i < activePointerCount; i++) {
                if ((pointerCoords[i].x == x) && (pointerCoords[i].y == y)) {
                    ptrIndex = i;
                    found = true;
                    break;
                }
            }
            assert found;
            pointerCoords[ptrIndex].pressure = 0f;

        } else {
            ptrIndex = activePointerCount;
            // Find new pointer id (lowest possible, since experiment shows high values don't work)
            int ptrId = 0;
            for (; ; ptrId++) {
                boolean collide = false;
                for (int i = 0; i < activePointerCount; i++) {
                    if (pointerProperties[i].id == ptrId) {
                        collide = true;
                        break;
                    }
                }
                if (!collide) break;

            }
            pointerProperties[ptrIndex].id = ptrId;

            pointerCoords[ptrIndex].x        = x;
            pointerCoords[ptrIndex].y        = y;
            pointerCoords[ptrIndex].pressure = 1f;
        }

        if (isKeyUp) {
            if (activePointerCount == 1) {
                action = MotionEvent.ACTION_UP;
            } else {
                // secondary pointers must use ACTION_POINTER_* ORed with the pointerIndex
                action = MotionEvent.ACTION_POINTER_UP | (ptrIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        } else {
            if (activePointerCount == 0) {
                action = MotionEvent.ACTION_DOWN;
                downTime = now;
            } else {
                action = MotionEvent.ACTION_POINTER_DOWN | (ptrIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
            }
        }

        assert downTime != 0;
        // Event injection
        Ln.d("Injecting event");
        // Even if the pointer is up, we still need its data (I guess)
        int inputPtrCount = (isKeyUp ? activePointerCount : activePointerCount+1);
        MotionEvent motionEvent = MotionEvent
            .obtain(downTime, now, action, inputPtrCount,
                    pointerProperties, pointerCoords,
                    0, 0, 1f, 1f, DEFAULT_DEVICE_ID, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0);
        Ln.d(motionEvent.toString());
        boolean result = device.injectEvent(motionEvent);

        if (isKeyUp) {
            // Remove the inactive pointer (must be after event injection)
            for (int i=ptrIndex; i < activePointerCount-1; i++) {
                // shallow copy the important stuff
                pointerProperties[i].id   = pointerProperties[i+1].id;
                pointerCoords[i].x        = pointerCoords[i+1].x;
                pointerCoords[i].y        = pointerCoords[i+1].y;
                pointerCoords[i].pressure = pointerCoords[i+1].pressure;
            }
        }

        if (isKeyUp) {
            activePointerCount--;
        } else {
            activePointerCount++;
        }

        return result;
    }
}
