using UnityEngine.EventSystems;

public class KeyboardChooserButton : KeyboardButton
{
    public override void OnKeyTrigger()
    {
        NativeInterface.OpenSystemKeyboardChooser();
    }
}
