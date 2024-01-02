using UnityEngine;

public class SpeechButton : ViewDependantButton
{
    public override void OnKeyTrigger() => _controller.ToggleMic();
}
