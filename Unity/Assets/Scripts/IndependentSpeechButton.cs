using UnityEngine;

public class IndependentSpeechButton : KeyboardButton
{
    [SerializeField]
    private KeyboardController _controller = null;

    public override void OnKeyTrigger() => _controller.ToggleMic();
}
