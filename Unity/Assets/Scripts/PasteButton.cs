using UnityEngine.EventSystems;

public class PasteButton : KeyboardButton
{
    public override void OnKeyTrigger()
    {
        NativeInterface.Paste();
    }
}
