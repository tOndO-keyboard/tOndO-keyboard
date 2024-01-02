using UnityEngine.EventSystems;

public class RedoButton : KeyboardButton
{
    public override void OnKeyTrigger()
    {
        NativeInterface.Redo();
    }
}
