using System.Collections.Generic;
using UnityEngine;

public class TopBarPositionController : MonoBehaviour
{
    [SerializeField]
    private List<Transform> _flippables;

    private SettingsManager settingsManager;

    private bool IsBottom
    {
        get
        {
            return transform.GetSiblingIndex() == (transform.parent.childCount - 1);
        }
    }

    private void Start()
    {
        NativeInterface.InputViewStarted += OnInputViewStarted;
    }

    private void OnEnable()
    {
        settingsManager = SettingsManager.Instance;

        OnInputViewStarted();
    }

    private void OnInputViewStarted(string s = "")
    {
        bool wentBottom = settingsManager.TopBarWentBottom;

        if(wentBottom)
        {
            GoToBottom();
        }
        else
        {
            GoToTop();
        }
    }

    private void GoToBottom()
    {
        if(IsBottom)
        {
            return;
        }

        transform.SetAsLastSibling();
        _flippables.ForEach(f => f.localScale = Vector3.one);
        Camera.main.backgroundColor = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.KEYBOARD_BACKGROUND);
    }

    private void GoToTop()
    {
        if(!IsBottom)
        {
            return;
        }

        transform.SetAsFirstSibling();
        _flippables.ForEach(f => f.localScale = new Vector3(1, -1, 1));
        Camera.main.backgroundColor = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.ACTION_BAR_BACKGROUND);
    }
        
}
