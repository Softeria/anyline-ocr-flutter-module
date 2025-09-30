package io.anyline.flutter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import io.anyline2.AnylineSdk;
import io.anyline2.core.LicenseException;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * AnylinePlugin
 */
public class AnylinePlugin implements
        FlutterPlugin,
        MethodCallHandler,
        PluginRegistry.ActivityResultListener,
        ResultReporter.OnResultListener,
        ActivityAware
{

    private MethodChannel channel;

    private String customModelsPath = "flutter_assets";
    private String viewConfigsPath = "flutter_assets";

    private String configJson;
    private JSONObject configObject;
    private Activity activity;
    private MethodChannel.Result result;
    private Context context;

    private BinaryMessenger messenger;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        messenger = flutterPluginBinding.getBinaryMessenger();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "anyline_plugin");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();

        // Register the embedded view factory
        flutterPluginBinding.getPlatformViewRegistry().registerViewFactory(
                "anyline_embedded_plugin", new NativeScanViewFactory(messenger));
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        this.result = result;

        try {
            switch (call.method) {
                case Constants.METHOD_GET_SDK_VERSION:
                    result.success("54.4.0");
                    break;

                case Constants.METHOD_SET_LICENSE_KEY:
                    String licenseKey = call.argument(Constants.EXTRA_LICENSE_KEY);
                    boolean enableOfflineCache = Boolean.TRUE.equals(call.argument(Constants.EXTRA_ENABLE_OFFLINE_CACHE));

                    try {
                        AnylineSdk.init(licenseKey, context);
                        result.success(true);
                    } catch (LicenseException e) {
                        result.error(Constants.EXCEPTION_LICENSE, e.getMessage(), null);
                    }
                    break;

                case Constants.METHOD_SET_CUSTOM_MODELS_PATH:
                    customModelsPath = call.argument(Constants.EXTRA_CUSTOM_MODELS_PATH);
                    result.success(null);
                    break;

                case Constants.METHOD_SET_VIEW_CONFIGS_PATH:
                    viewConfigsPath = call.argument(Constants.EXTRA_VIEW_CONFIGS_PATH);
                    result.success(null);
                    break;

                case Constants.METHOD_START_ANYLINE:
                    // For embedded view, scanning is handled by the embedded component
                    // This method is kept for compatibility but scanning should use embedded view
                    result.error(Constants.EXCEPTION_ANYLINE, "Use embedded view for scanning", null);
                    break;

                case Constants.METHOD_GET_APPLICATION_CACHE_PATH:
                    result.success(getApplicationCachePath());
                    break;

                default:
                    result.notImplemented();
                    break;
            }
        } catch (Exception e) {
            result.error(Constants.EXCEPTION_ANYLINE, e.getMessage(), null);
        }
    }

    private String getApplicationCachePath() {
        if (context != null) {
            File cacheDir = context.getCacheDir();
            return cacheDir.getAbsolutePath();
        }
        return null;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        // No activity-based scanning for embedded view approach
        return false;
    }

    @Override
    public void onResult(Object result, boolean isCanceled) {
        if (this.result != null) {
            if (isCanceled) {
                this.result.success("Canceled");
            } else {
                this.result.success(result);
            }
            this.result = null;
        }
    }

    @Override
    public void onError(String error) {
        if (this.result != null) {
            this.result.error(Constants.EXCEPTION_ANYLINE, error, null);
            this.result = null;
        }
    }

    @Override
    public void onCancel() {
        if (this.result != null) {
            this.result.success("Canceled");
            this.result = null;
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }
}
