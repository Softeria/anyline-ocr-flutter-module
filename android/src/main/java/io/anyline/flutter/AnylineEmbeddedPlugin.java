package io.anyline.flutter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.anyline2.Event;
import io.anyline2.ScanResult;
import io.anyline2.camera.CameraView;
import io.anyline2.viewplugin.ScanViewPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class AnylineEmbeddedPlugin extends PlatformViewFactory {
    private final BinaryMessenger messenger;

    public AnylineEmbeddedPlugin(BinaryMessenger messenger) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;
    }

    @Override
    public PlatformView create(Context context, int viewId, Object args) {
        Map<String, Object> params = args instanceof Map ? (Map<String, Object>) args : new HashMap<>();
        return new ScannerView(context, messenger, viewId, params);
    }

    static class ScannerView extends FrameLayout implements PlatformView, MethodChannel.MethodCallHandler {
        private static final String TAG = "AnylineEmbedded";

        private CameraView cameraView;
        private ScanViewPlugin scanPlugin;
        private EventChannel.EventSink eventSink;
        private boolean isScanning = false;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private boolean flashOn = false;

        ScannerView(Context context, BinaryMessenger messenger, int viewId, Map<String, Object> params) {
            super(context);

            new MethodChannel(messenger, "anyline_embedded/methods_" + viewId).setMethodCallHandler(this);
            new EventChannel(messenger, "anyline_embedded/events_" + viewId).setStreamHandler(
                    new EventChannel.StreamHandler() {
                        @Override
                        public void onListen(Object arguments, EventChannel.EventSink events) {
                            eventSink = events;
                        }

                        @Override
                        public void onCancel(Object arguments) {
                            eventSink = null;
                        }
                    }
            );

            cameraView = new CameraView(context);
            addView(cameraView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

            Boolean initialFlashState = (Boolean) params.get("initialFlashState");
            if (initialFlashState != null) {
                flashOn = initialFlashState.booleanValue();
            }

            String config = (String) params.get("config");
            if (config != null) {
                try {
                    initScanner(config);
                } catch (Exception e) {
                    Log.e(TAG, "Init failed", e);
                    sendEvent("error", e.getMessage());
                }
            }
        }

        private void initScanner(String configJson) throws Exception {
            scanPlugin = new ScanViewPlugin(new JSONObject(configJson));
            scanPlugin.subscribeToImageProvider(cameraView);

            scanPlugin.resultReceived = new Event<ScanResult>() {
                @Override
                public void eventReceived(ScanResult result) {
                    sendEvent("result", result.getResult().toString());
                }
            };

            scanPlugin.errorReceived = new Event<JSONObject>() {
                @Override
                public void eventReceived(JSONObject error) {
                    sendEvent("error", error.toString());
                }
            };
        }

        private void sendEvent(String type, String data) {
            mainHandler.post(() -> {
                if (eventSink != null) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("type", type);
                    event.put("data", data);
                    eventSink.success(event);
                }
            });
        }

        @Override
        public void onMethodCall(MethodCall call, MethodChannel.Result result) {
            try {
                switch (call.method) {
                    case "start":
                        if (!isScanning && scanPlugin != null) {
                            cameraView.openCameraInBackground();
                            try {
                                cameraView.setFlashOn(flashOn);
                            } catch (Throwable ignored) {
                            }
                            scanPlugin.start();
                            isScanning = true;
                        }
                        result.success(null);
                        break;

                    case "stop":
                        if (isScanning && scanPlugin != null) {
                            scanPlugin.stop();
                            cameraView.releaseCameraInBackground();
                            isScanning = false;
                        }
                        result.success(null);
                        break;

                    case "init":
                        String config = call.argument("config");
                        if (config != null) {
                            initScanner(config);
                        }
                        result.success(null);
                        break;

                    case "setFlashOn": {
                        Boolean on = call.argument("on");
                        if (on == null) {
                            result.error("ARG_ERROR", "Missing 'on' bool", null);
                            break;
                        }
                        flashOn = on;
                        try {
                            cameraView.setFlashOn(flashOn);
                        } catch (Throwable ignored) {
                        }
                        result.success(flashOn);
                        break;
                    }

                    case "toggleFlash": {
                        flashOn = !flashOn;
                        try {
                            cameraView.setFlashOn(flashOn);
                        } catch (Throwable ignored) {
                        }
                        result.success(flashOn);
                        break;
                    }

                    case "getFlashOn": {
                        result.success(flashOn);
                        break;
                    }

                    default:
                        result.notImplemented();
                }
            } catch (Exception e) {
                result.error("ERROR", e.getMessage(), null);
            }
        }

        @Override
        public View getView() {
            return this;
        }

        @Override
        public void dispose() {
            if (scanPlugin != null) {
                try {
                    scanPlugin.stop();
                    scanPlugin.dispose();
                } catch (Exception ignored) {
                }
            }
            try {
                cameraView.releaseCameraInBackground();
            } catch (Exception ignored) {
            }
        }
    }
}
