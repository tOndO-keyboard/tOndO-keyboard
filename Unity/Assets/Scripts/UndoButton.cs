using UnityEngine.EventSystems;

public class UndoButton : KeyboardButton
{
    public override void OnKeyTrigger()
    {
        NativeInterface.Undo();
    }
}
