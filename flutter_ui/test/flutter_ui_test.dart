import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_ui/flutter_ui.dart';
import 'package:flutter_ui/flutter_ui_platform_interface.dart';
import 'package:flutter_ui/flutter_ui_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFlutterUiPlatform
    with MockPlatformInterfaceMixin
    implements FlutterUiPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FlutterUiPlatform initialPlatform = FlutterUiPlatform.instance;

  test('$MethodChannelFlutterUi is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFlutterUi>());
  });

  test('getPlatformVersion', () async {
    FlutterUi flutterUiPlugin = FlutterUi();
    MockFlutterUiPlatform fakePlatform = MockFlutterUiPlatform();
    FlutterUiPlatform.instance = fakePlatform;

    expect(await flutterUiPlugin.getPlatformVersion(), '42');
  });
}
