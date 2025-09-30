import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

class AnylineEmbeddedPlugin extends StatefulWidget {
  const AnylineEmbeddedPlugin({
    Key? key,
    required this.config,
    this.onResult,
    this.onError,
    this.autoStart = true,
    this.initialFlashState = false,
  }) : super(key: key);

  final Map<String, dynamic> config;
  final Function(Map<String, dynamic> result)? onResult;
  final Function(String error)? onError;
  final bool autoStart;
  final bool initialFlashState;

  @override
  State<AnylineEmbeddedPlugin> createState() => AnylineEmbeddedPluginState();
}

class AnylineEmbeddedPluginState extends State<AnylineEmbeddedPlugin> {
  EventChannel? _events;
  StreamSubscription? _subscription;

  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      return AndroidView(
        viewType: 'anyline_embedded_plugin',
        creationParams: {
          'config': widget.config,
        },
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: (viewId) {
          _events = EventChannel('anyline_embedded_plugin_$viewId');

          _subscription = _events!.receiveBroadcastStream().listen((event) {
            if (event is Map<Object?, Object?>) {
              final Map<String, dynamic> result = Map.from(event);
              widget.onResult?.call(result);
            }
          }, onError: (error) {
            widget.onError?.call(error.toString());
          });
        },
      );
    } else if (Platform.isIOS) {
      return UiKitView(
        viewType: 'anyline_embedded_plugin',
        creationParams: {
          'config': widget.config,
        },
        creationParamsCodec: const StandardMessageCodec(),
        onPlatformViewCreated: (viewId) {
          _events = EventChannel('anyline_embedded_plugin_$viewId');

          _subscription = _events!.receiveBroadcastStream().listen((event) {
            if (event is Map<Object?, Object?>) {
              final Map<String, dynamic> result = Map.from(event);
              widget.onResult?.call(result);
            }
          }, onError: (error) {
            widget.onError?.call(error.toString());
          });
        },
      );
    }

    return const Center(child: Text('Platform not supported'));
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }
}
