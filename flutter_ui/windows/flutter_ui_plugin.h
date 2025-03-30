#ifndef FLUTTER_PLUGIN_FLUTTER_UI_PLUGIN_H_
#define FLUTTER_PLUGIN_FLUTTER_UI_PLUGIN_H_

#include <flutter/method_channel.h>
#include <flutter/plugin_registrar_windows.h>

#include <memory>

namespace flutter_ui {

class FlutterUiPlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows *registrar);

  FlutterUiPlugin();

  virtual ~FlutterUiPlugin();

  // Disallow copy and assign.
  FlutterUiPlugin(const FlutterUiPlugin&) = delete;
  FlutterUiPlugin& operator=(const FlutterUiPlugin&) = delete;

  // Called when a method is called on this plugin's channel from Dart.
  void HandleMethodCall(
      const flutter::MethodCall<flutter::EncodableValue> &method_call,
      std::unique_ptr<flutter::MethodResult<flutter::EncodableValue>> result);
};

}  // namespace flutter_ui

#endif  // FLUTTER_PLUGIN_FLUTTER_UI_PLUGIN_H_
