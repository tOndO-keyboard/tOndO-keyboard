#include "include/flutter_ui/flutter_ui_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "flutter_ui_plugin.h"

void FlutterUiPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  flutter_ui::FlutterUiPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
