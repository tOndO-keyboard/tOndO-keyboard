using UnityEngine;

public class EmojiButton : KeyboardButton
{
    [SerializeField]
    private KeyboardController _controller = null;

    public override void OnKeyTrigger() => _controller.ToggleEmoji();
}
