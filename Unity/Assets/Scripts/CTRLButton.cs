using UnityEngine;

public class CTRLButton : KeyboardButton
{
    [SerializeField]
    private KeyboardController _controller = null;

    public override void OnKeyTrigger() => _controller.ToggleCtrlMode();
}
