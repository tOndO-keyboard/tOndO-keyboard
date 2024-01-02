using UnityEngine.EventSystems;

public class SelectAllButton : KeyboardButton
{
    public override void OnKeyTrigger()
    {
        NativeInterface.SelectAll();
    }
}
