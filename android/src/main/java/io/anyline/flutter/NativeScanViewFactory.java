package io.anyline.flutter;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class NativeScanViewFactory extends PlatformViewFactory {
    private final BinaryMessenger messenger;

    public NativeScanViewFactory(BinaryMessenger messenger) {
        super(StandardMessageCodec.INSTANCE);
        this.messenger = messenger;
    }

    @Override
    public PlatformView create(Context context, int viewId, Object args) {
        Map<String, Object> params = args instanceof Map ? (Map<String, Object>) args : new HashMap<>();
        return new NativeScanView(context, viewId, params, messenger);
    }
}
