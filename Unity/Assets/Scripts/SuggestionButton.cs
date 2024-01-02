using TMPro;
using UnityEngine;

[RequireComponent(typeof(TMP_Text))]
public class SuggestionButton : KeyboardButton
{
    public event System.Action<SuggestionButton> SuggestionClick;

    [SerializeField]
    private TMP_Text _text = null;

    public string Text 
    {
        get => _text.text;
        set => _text.text = value;
    }

    public override void OnKeyTrigger() => SuggestionClick?.Invoke(this);

#if UNITY_EDITOR
    private void OnValidate()
    {
        if (_text == null) _text = GetComponent<TMP_Text>();
    }
#endif
}
