using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class HideButton : KeyboardButton
{
    public delegate void HIdeChangedStateEventDelegate(HideButton source, bool hidden);

    public static event HIdeChangedStateEventDelegate HideChangedState;

    [SerializeField]
    private RectTransform VerticalLayoutRect;

    [SerializeField]
    private RectTransform HiddenTopRect;

    [SerializeField]
    private RectTransform TopRect;

    [SerializeField]
    private RectTransform BottomRect;

    [SerializeField]
    private bool isUnhide;

    [SerializeField]
    private bool isTop;

    private SettingsManager settingsManager;
    private CanvasScaler canvasScaler;

    private bool Hidden
    {
        get 
        {
            return !TopRect.gameObject.activeSelf;
        }
    }

    private void Start()
    {
        settingsManager = SettingsManager.Instance;

        canvasScaler = GetComponentInParent<CanvasScaler>();

        HideButton.HideChangedState += OnHideStateChanged;
        NativeInterface.InputViewStarted += OnInputViewStarted;

        bool hidden = settingsManager.TopBarHidden;

        if (hidden)
        {
            Hide();
        }
        else
        {
            Unhide();
        }

        OnInputViewStarted();
    }

    private void OnInputViewStarted(string s = "")
    {
        bool barHidden = settingsManager.TopBarHidden;
        SetVisibility(barHidden);
    }

    private void OnHideStateChanged(HideButton source, bool barHidden)
    {
        SetVisibility(barHidden);
    }

    private void SetVisibility(bool barHidden)
    {
        if (isTop && settingsManager.TopBarWentBottom)
        {
            gameObject.SetActive(false);
            return;
        }

        if (!isTop && !settingsManager.TopBarWentBottom)
        {
            gameObject.SetActive(false);
            return;
        }

        if (isUnhide)
        {
            gameObject.SetActive(barHidden);
        }
        else
        {
            gameObject.SetActive(!barHidden);
        }
    }

    private void Hide()
    {
        TopRect.gameObject.SetActive(false);
        HiddenTopRect.gameObject.SetActive(true);
        VerticalLayoutRect.sizeDelta = new Vector2(VerticalLayoutRect.sizeDelta.x, BottomRect.rect.height + HiddenTopRect.rect.height);
        canvasScaler.referenceResolution = new Vector2(BottomRect.rect.width, BottomRect.rect.height + HiddenTopRect.rect.height);
        NativeInterface.SetKeyboardProportion(CanvasScalerManager.HIDDEN_TOP_BAR_PROPORTION);
        HideChangedState?.Invoke(this, true);
    }

    private void Unhide()
    {
        TopRect.gameObject.SetActive(true);
        HiddenTopRect.gameObject.SetActive(false);
        VerticalLayoutRect.sizeDelta = new Vector2(VerticalLayoutRect.sizeDelta.x, BottomRect.rect.height + TopRect.rect.height);
        canvasScaler.referenceResolution = new Vector2(BottomRect.rect.width, BottomRect.rect.height + TopRect.rect.height);
        NativeInterface.SetKeyboardProportion(CanvasScalerManager.DEFAULT_PROPORTION);
        HideChangedState?.Invoke(this, false);
    }


    public override void OnKeyTrigger()
    {
        if(isUnhide)
        {
            Unhide();
            settingsManager.TopBarHidden = false;
            gameObject.SetActive(false);
        }
        else
        {
            Hide();
            settingsManager.TopBarHidden = true;
        }
    }
}
