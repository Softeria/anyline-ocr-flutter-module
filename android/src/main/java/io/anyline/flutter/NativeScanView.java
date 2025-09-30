package io.anyline.flutter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.anyline2.Event;
import io.anyline2.ScanResult;
import io.anyline2.view.ScanView;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.platform.PlatformView;

@SuppressLint("InflateParams")
public class NativeScanView implements PlatformView, EventChannel.StreamHandler {
    private final View view;
    private ScanView anylineScanView;
    private EventChannel.EventSink event;

    public NativeScanView(Context context, int viewId, Map<String, Object> params, BinaryMessenger messenger) {
        view = LayoutInflater.from(context).inflate(R.layout.embedded_scan_view, null);
        anylineScanView = view.findViewById(R.id.anyline_scan_view);

        EventChannel eventChannel = new EventChannel(messenger, "anyline_embedded_plugin_" + viewId);
        eventChannel.setStreamHandler(this);

        initializeScanView((Map<String, Object>) params.get("config"));
    }

    @Override
    public View getView() {
        return view;
    }

    private void initializeScanView(Map<String, Object> viewConfig) {
        if (viewConfig != null) {
            anylineScanView.init(new JSONObject(viewConfig));
            anylineScanView.start();
        }
        addResultListener();
    }

    private void addResultListener() {
        if (anylineScanView != null) {
            anylineScanView.getScanViewPlugin().resultReceived = new Event<ScanResult>() {
                @Override
                public void eventReceived(ScanResult scanResult) {
                    onResult(scanResult);
                }
            };
        }
    }

    private void onResult(ScanResult scanResult) {
        if (event != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("result", scanResult.getResult().toString());
            result.put("cutoutImage", scanResult.getCutoutImage() != null ? scanResult.getCutoutImage().save() : "");
            // Send result back to flutter using event channel
            event.success(result);
        }
    }

    /**
     * Listener for event channel
     * */
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        event = events;
    }

    @Override
    public void onCancel(Object arguments) {
        event = null;
    }

    @Override
    public void dispose() {
        if (anylineScanView != null) {
            anylineScanView.stop();
        }
        if (event != null) {
            event.endOfStream();
        }
    }
}