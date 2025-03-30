
import 'flutter_ui_platform_interface.dart';

class FlutterUi {
  Future<String?> getPlatformVersion() {
    return FlutterUiPlatform.instance.getPlatformVersion();
  }
}
