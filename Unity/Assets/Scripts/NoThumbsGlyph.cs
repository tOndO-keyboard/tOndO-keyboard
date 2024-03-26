using TMPro;
using Tweens;
using UnityEngine;
using UnityEngine.UI;
using static ShiftButton;

[RequireComponent(typeof(Image))]
public class NoThumbsGlyph : MonoBehaviour
{
    [SerializeField]
    private GameObject previewPopupPrefab;

    [SerializeField]
    private GameObject previewPopupSmallPrefab;

    [SerializeField]
    private bool isOnVeryTopRow;

    private bool isUppercase = true;

    protected NativeInterface nativeInterface;

    public delegate void GlyphCommittedEventDelegate(string selectedGlyph);
    public static event GlyphCommittedEventDelegate GlyphCommitted;

    public delegate void GlyphCommittingEventDelegate(string precedingGlyph, string selectedGlyph);
    public static event GlyphCommittingEventDelegate GlyphCommitting;

    public int AngularIndex
    {
        get
        {
            return transform.GetSiblingIndex();
        }
    }

    private Image background;
    private TMP_Text label;

    private Color textInitialColor;
    private Color backgroundInitialColor;

    private GameObject previewPopupGameObject;

    private SettingsManager settingsManager;

    private string defaultLable;
    private bool temporaryLabelSet = false;

    private void Awake()
    {
        settingsManager = SettingsManager.Instance;

        nativeInterface = NativeInterface.Instance;

        ShiftButton.ShiftChangedState += OnShiftStateChanged; 
        NativeInterface.InputViewStarted += OnInputViewStarted;

        background = GetComponent<Image>();
        backgroundInitialColor = background.color;

        label = GetComponentInChildren<TMP_Text>();

        textInitialColor = label.color;

        PaletteElementColorApplier paletteElementColorApplier = GetComponentInChildren<PaletteElementColorApplier>();
        if (paletteElementColorApplier != null)
        {
            paletteElementColorApplier.GraphicColorChanged += OnThemeChanged;
        }
    }

    private void OnInputViewStarted(string s = "")
    {
        SetCase(isUppercase);
    }

    private void OnShiftStateChanged(ShiftButton source, ShiftState state)
    {
        isUppercase = state != ShiftState.notPressed;
        SetCase(isUppercase);
    }

    private void SetCase(bool upper)
    {
        char[] chars = GetLabel().ToCharArray();
        if (chars.Length == 1 && char.IsLetter(chars[0]))
        {
            label.text = (upper ? GetLabel().ToUpper() : GetLabel().ToLower());
        }
    }

    private void OnThemeChanged()
    {
        backgroundInitialColor = background.color;
        textInitialColor = label.color;
    }

    public string GetLabel()
    {
        return label.text.Replace("␣", " ");
    }

    public void SetTemporaryLabel(string l)
    {
        defaultLable = label.text;
        label.text = l;
        label.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.GLYPH_CONSONANT);
        temporaryLabelSet = true;
    }

    public void ResetTemporaryLabel()
    {
        label.text = defaultLable;
        label.color = textInitialColor;
        temporaryLabelSet = false;
    }

    public void OnTweenFillAmountFinished()
    {
        GlyphCommitting?.Invoke(nativeInterface.GetPrecedingCharacter(1), label.text);
        nativeInterface.CommitString(label.text);
        GlyphCommitted?.Invoke(label.text);
    }

    public void SetHilight(bool hilight, bool useAlsoBackground = true, bool showPreviewPopup = true)
    {
        if (hilight)
        {
            if (showPreviewPopup) useAlsoBackground = false;
            if (useAlsoBackground) background.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.GLYPH_SELECTED_BACKGROUND);

            label.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.GLYPH_SELECTED);

            if (showPreviewPopup)
            {
                if (isOnVeryTopRow && (settingsManager.TopBarHidden || settingsManager.TopBarWentBottom))
                {
                    if (previewPopupGameObject == null)
                    {
                        previewPopupGameObject = Instantiate(previewPopupSmallPrefab);
                    }
                }

                if (previewPopupGameObject == null)
                {
                    previewPopupGameObject = Instantiate(previewPopupPrefab);
                    previewPopupGameObject.GetComponentInChildren<TweenFillAmount>().SetOnFinished(OnTweenFillAmountFinished);
                    previewPopupGameObject.GetComponentInChildren<TweenFillAmount>().Play(true, true);
                }
                previewPopupGameObject.transform.SetParent(transform, false);
                previewPopupGameObject.GetComponent<RectTransform>().anchoredPosition = Vector3.zero;
                previewPopupGameObject.GetComponent<PreviewPopup>().SetLabel(label.text, Mathf.RoundToInt(label.fontSize));
            }
        }
        else
        {
            background.color = backgroundInitialColor;

            label.color = temporaryLabelSet ? ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.GLYPH_CONSONANT) : textInitialColor;

            if (previewPopupGameObject != null)
            {
                previewPopupGameObject.GetComponentInChildren<TweenFillAmount>().Stop();
                Destroy(previewPopupGameObject);
                previewPopupGameObject = null;
            }
        }
    }
}

