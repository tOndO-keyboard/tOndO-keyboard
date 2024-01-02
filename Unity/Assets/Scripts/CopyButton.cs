using UnityEngine.EventSystems;

public class CopyButton : KeyboardButton
{
    public override void OnKeyTrigger()
    {
        NativeInterface.Copy();
    }
}
