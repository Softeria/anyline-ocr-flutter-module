import 'dart:async';

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
  }) : super(key: key);

  final String config;
  final Function(String result)? onResult;
  final Function(String error)? onError;
  final bool autoStart;

  @override
  State<AnylineEmbeddedPlugin> createState() => AnylineEmbeddedPluginState();
}

class AnylineEmbeddedPluginState extends State<AnylineEmbeddedPlugin> {
  MethodChannel? _method;
  EventChannel? _events;
  StreamSubscription? _subscription;

  Future<void> start() async => await _method?.invokeMethod('start');

  Future<void> stop() async => await _method?.invokeMethod('stop');

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform != TargetPlatform.android) {
      return const Center(child: Text('Android only for now'));
    }

    return AndroidView(
      viewType: 'anyline_embedded_plugin',
      creationParams: {'config': widget.config},
      creationParamsCodec: const StandardMessageCodec(),
      onPlatformViewCreated: (viewId) {
        _method = MethodChannel('anyline_embedded/methods_$viewId');
        _events = EventChannel('anyline_embedded/events_$viewId');

        _subscription = _events!.receiveBroadcastStream().listen((event) {
          if (event is Map) {
            final type = event['type'];
            final data = event['data'];

            if (type == 'result') {
              widget.onResult?.call(data);
            } else if (type == 'error') {
              widget.onError?.call(data);
            }
          }
        });

        if (widget.autoStart) {
          Future.delayed(const Duration(milliseconds: 500), () => start());
        }
      },
    );
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _method?.invokeMethod('stop');
    super.dispose();
  }
}
