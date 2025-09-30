import 'dart:convert';
import 'dart:typed_data';

import 'package:anyline_plugin/anyline_embedded_plugin.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class EmbeddedDemo extends StatefulWidget {
  const EmbeddedDemo({Key? key}) : super(key: key);

  @override
  State<EmbeddedDemo> createState() => _EmbeddedDemoState();
}

class _EmbeddedDemoState extends State<EmbeddedDemo> {
  String? _result;
  Uint8List? _cutoutImage;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Embedded Anyline Demo'),
      ),
      body: Column(
        children: [
          Expanded(
            flex: 2,
            child: FutureBuilder<Map<String, dynamic>>(
              future: _loadConfig(),
              builder: (context, snapshot) {
                if (!snapshot.hasData) {
                  return const Center(child: CircularProgressIndicator());
                }

                return AnylineEmbeddedPlugin(
                  config: snapshot.data!,
                  onResult: _onResult,
                  onError: _onError,
                );
              },
            ),
          ),
          Expanded(
            flex: 1,
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Scan Result:',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  if (_result != null) ...[
                    Text(
                      _result!,
                      style: const TextStyle(fontSize: 14),
                    ),
                    const SizedBox(height: 8),
                  ],
                  if (_cutoutImage != null) ...[
                    const Text(
                      'Cutout Image:',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    Image.memory(
                      _cutoutImage!,
                      height: 100,
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<Map<String, dynamic>> _loadConfig() async {
    final configString = await rootBundle.loadString('config/AnalogMeterConfig.json');
    return jsonDecode(configString) as Map<String, dynamic>;
  }

  void _onResult(Map<String, dynamic> result) {
    setState(() {
      _result = result['result']?.toString();

      // Handle cutout image
      if (result['cutoutImage'] != null) {
        final String imagePath = result['cutoutImage'];
        // For now, just show the path - in a real app you'd load the image
        // from the file path
        print('Cutout image saved at: $imagePath');
      }
    });
  }

  void _onError(String error) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Error: $error')),
    );
  }
}