using UnityEngine;
using UnityEngine.EventSystems;

public class SettingsButton : KeyboardButton
{
    public override void OnKeyTrigger()
    {
        NativeInterface.OpenKeyboardOptions();
    }
}
