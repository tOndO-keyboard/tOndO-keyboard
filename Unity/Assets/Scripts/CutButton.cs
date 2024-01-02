using UnityEngine.EventSystems;

public class CutButton : KeyboardButton
{
    public override void OnKeyTrigger()
    {
        NativeInterface.Cut();
    }
}
