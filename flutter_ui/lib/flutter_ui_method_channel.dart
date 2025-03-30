import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_ui_platform_interface.dart';

/// An implementation of [FlutterUiPlatform] that uses method channels.
class MethodChannelFlutterUi extends FlutterUiPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_ui');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
