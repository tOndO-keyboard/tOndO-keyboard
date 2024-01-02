using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class SuggestionsBar : MonoBehaviour
{
    public event System.Action SuggestionSubmitted;
    public event System.Action OnBack;

    [SerializeField]
    private Button _backButton = null;

    [SerializeField]
    private SuggestionButton[] _suggestions = null;
    [SerializeField]
    private GameObject[] _separators = null;

    private void Awake()
    {
        foreach (var button in _suggestions)
        {
            button.Text = "";
            button.SuggestionClick += OnSuggestionClick;
        }
        _backButton.onClick.AddListener(() => OnBack());
    }

    private void OnSuggestionClick(SuggestionButton source) 
    {
        NativeInterface.Instance.CommitCorrection(source.Text);
        SuggestionSubmitted?.Invoke();
    }

    public void SetSuggestions(IEnumerable<string> suggestions)
    {
        var list = new List<string>(suggestions);
        for (int i = 0; i < _suggestions.Length; i++)
        {
            string suggestion = "";
            if (i < list.Count) suggestion = list[i];
            _suggestions[i].Text = suggestion;
            _suggestions[i].gameObject.SetActive(!string.IsNullOrEmpty(suggestion));
            if (i > 0) _separators[i - 1].SetActive(!string.IsNullOrEmpty(suggestion));
        }
    }
}
