using TMPro;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class ClipboardCommitButton : ScrollRectKeyboardButton, IDeselectHandler
{
    public event System.Action<ClipboardCommitButton, bool> LongPress;

    [SerializeField]
    private TMP_Text _text = null;
    [SerializeField]
    private Image _functionImage = null;
    [SerializeField]
    private bool _functionIsSave = false;

    private string _textValue;
    private bool _isFunctionActive = false;

    public string Text
    {
        get => _textValue;
        set
        {
            if (_textValue != null && _textValue.Equals(value, System.StringComparison.Ordinal)) return;
            _textValue = value;
            _text.text = value.Trim();
            IsFunctionActive = false;
        }
    }

    public bool IsFunctionActive
    {
        get => _isFunctionActive;
        set
        {
            _isFunctionActive = value;
            SetIcons();
        }
    }

    public int MacrosOverride { get; set; } = 0;

    private void Awake() => SetIcons();

    private void SetIcons()
    {
        var color = _text.color;
        color.a = System.Convert.ToInt32(!_isFunctionActive);
        _text.color = color;
        _functionImage.gameObject.SetActive(_isFunctionActive);
    }

    private void ButtonFunction()
    {
        if (!_functionIsSave) MacrosPersister.Instance.RemoveMacro(_textValue);
        else if (!MacrosPersister.Instance.ContainsMacro(_textValue))
        {
            int count = MacrosOverride > 0 ? MacrosOverride : MacrosPersister.Instance.Capacity;
            if (MacrosPersister.Instance.Count < count)
                MacrosPersister.Instance.AddMacro(_textValue);
            else Toast.Instance.Show(Localization.Instance.Localize(Localization.TOO_MANY_MACRO_TOAST));
            IsFunctionActive = false;
        }
    }

    public override void OnKeyTrigger()
    {
        if (_isFunctionActive)
        {
            ButtonFunction();
            return;
        }
        NativeInterface.Instance.CommitString(Text);

        MacrosPersister repo = MacrosPersister.Instance;
        if (repo.ContainsMacro(Text)) repo[Text]++;
    }

    public override void OnLongPressTrigger()
    {
        _isFunctionActive = !_isFunctionActive;
        SetIcons();
        LongPress?.Invoke(this, _isFunctionActive);
    }

    public void OnDeselect(BaseEventData eventData) => IsFunctionActive = false;
}
