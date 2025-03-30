import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_ui_method_channel.dart';

abstract class FlutterUiPlatform extends PlatformInterface {
  /// Constructs a FlutterUiPlatform.
  FlutterUiPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterUiPlatform _instance = MethodChannelFlutterUi();

  /// The default instance of [FlutterUiPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterUi].
  static FlutterUiPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterUiPlatform] when
  /// they register themselves.
  static set instance(FlutterUiPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
