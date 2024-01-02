public class MacrosButton : ViewDependantButton
{
    public override void OnKeyTrigger() => _controller.ToggleMacros();
}
