package lackhoa.akb;

import lackhoa.akb.wrappers.InputManager;
import lackhoa.akb.wrappers.ServiceManager;

import android.net.LocalSocketAddress;
import android.net.LocalSocket;
import android.os.IBinder;
import android.os.IInterface;
import android.os.SystemClock;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class Server {
    private static int bytesToInt(byte[] bytes) {
        return ((Byte.toUnsignedInt(bytes[0]) << 24)
                + (Byte.toUnsignedInt(bytes[1]) << 16)
                + (Byte.toUnsignedInt(bytes[2]) << 8)
                + (Byte.toUnsignedInt(bytes[3])));
    }

    public static void main(String... args) {
        Ln.initLogLevel(Ln.Level.DEBUG);
        ServiceManager serviceManager = new ServiceManager();
        InputManager inputManager = serviceManager.getInputManager();
        Device device = new Device();
        Controller controller = new Controller(device);

        LocalSocket localSocket = new LocalSocket();

        try {
            try {
                String SOCKET_NAME = "akb";
                localSocket.connect(new LocalSocketAddress(SOCKET_NAME));
                Ln.d("Connection opened!");
            } catch(IOException exception) {
                Ln.e("Can't open connection. IOException:" + exception.toString());
                return;
            }
            InputStream inputStream = null;
            try {
                inputStream = localSocket.getInputStream();
            } catch(IOException exception) {
                Ln.e("Can't obtain input stream:" + exception.toString());
                return;
            }
            byte[] keyEventBytes = new byte[4];
            byte[] xBytes = new byte[4];
            byte[] yBytes = new byte[4];
            try {
                while (true) {
                    int readKeyEvent = inputStream.read(keyEventBytes);
                    if (readKeyEvent == -1) /* End of stream */ {
                        break;
                    }

                    int readX = inputStream.read(xBytes);
                    if (readX == -1) /* End of stream */ {
                        break;
                    }

                    int readY = inputStream.read(yBytes);
                    if (readY == -1) /* End of stream */ {
                        break;
                    }

                    // big-endian conversion
                    int keyEvent = Server.bytesToInt(keyEventBytes);
                    int x        = Server.bytesToInt(xBytes);
                    int y        = Server.bytesToInt(yBytes);

                    Ln.d("keyEvent: " + String.valueOf(keyEvent));
                    Ln.d("x: " + String.valueOf(x));
                    Ln.d("y: " + String.valueOf(y));

                    controller.handleMessage(keyEvent, x, y);
                }
            } catch(IOException exception) {
                Ln.i("Can't read from input stream: " + exception.toString());
                return;
            }
        } finally {
            try {
                localSocket.close();
                Ln.i("Connection closed");
            } catch(IOException exception) {
                Ln.e("Can't close the socket: " + exception.toString());
            }
        }
    }
}
