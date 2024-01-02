using TMPro;
using UnityEngine;
using UnityEngine.UI;
using static ShiftButton;

[RequireComponent(typeof(Image))]
public class TondoGlyph : MonoBehaviour
{
    [SerializeField]
    private GameObject previewPopupPrefab;

    [SerializeField]
    private GameObject previewPopupSmallPrefab;

    [SerializeField]
    public bool FollowShiftStateForCase = false;

    [SerializeField]
    public bool IsDiacriticizer = false;

    [HideInInspector]
    public bool IsLeftDiacriticizer = false;

    [SerializeField]
    private bool isOnVeryTopRow;

    [SerializeField]
    private LayoutType parentLayoutType;

    [SerializeField]
    private AnalogStickPosition parentAnalogStickPosition;

    [SerializeField]
    private CardinalDirection cardinalDirection;
    public CardinalDirection CardinalDirection
    {
        get
        {
            if (!directionCalculated) CalculateDirection();
            return cardinalDirection;
        }
    }

    private bool directionCalculated = false;

    private Image background;
    private TMP_Text label;

    private Color textInitialColor;
    private Color backgroundInitialColor;

    private GameObject previewPopupGameObject;

    private SettingsManager settingsManager;

    private DiacriticsCustomization diacriticsCustomization;

    private string defaultLable;
    private string initialLable;
    private bool temporaryLabelSet = false;

    private bool isUppercase = true;

    private void Awake()
    {
        settingsManager = SettingsManager.Instance;

        background = GetComponent<Image>();
        backgroundInitialColor = background.color;

        label = GetComponentInChildren<TMP_Text>();

        textInitialColor = label.color;

        if(FollowShiftStateForCase)
        {
            ShiftButton.ShiftChangedState += OnShiftStateChanged;
        }

        if(IsDiacriticizer)
        {
            diacriticsCustomization = DiacriticsCustomization.Instance;

            NativeInterface.CursorPositionChanged += UpdateLabelIfNeeded;

            UpdateLabelIfNeeded(NativeInterface.Instance.GetPrecedingCharacter(1));
        }

        PaletteElementColorApplier paletteElementColorApplier = GetComponentInChildren<PaletteElementColorApplier>();
        if (paletteElementColorApplier != null)
        {
            paletteElementColorApplier.GraphicColorChanged += OnThemeChanged;
        }

        parentLayoutType = GetComponentInParent<TondosLayout>().LayoutType;
        parentAnalogStickPosition = GetComponentInParent<TondoAnalogStick>().AnalogStickPosition;

        if (!IsDiacriticizer)
        {
            NativeInterface.InputViewStarted += OnInputViewStarted;
            initialLable = label.text;
            getCustomizedCharacter();
        }
    }

    private void OnInputViewStarted(string s = "")
    {
        getCustomizedCharacter();
    }

    private void getCustomizedCharacter()
    {
        if (IsDiacriticizer) return;

        string customizedCharacter = CharacterCustomization.Instance.Get(parentLayoutType, parentAnalogStickPosition, CardinalDirection).Replace(" ", "␣");
        if (!string.IsNullOrEmpty(customizedCharacter))
        {
            label.text = customizedCharacter;
        }
        else
        {
            label.text = initialLable;
        }

        if (label.text.Equals("␣"))
        {
            label.fontStyle = FontStyles.Bold;
        }
        else
        {
            label.fontStyle = FontStyles.Normal;
        }

        if (FollowShiftStateForCase) SetCase(isUppercase);
    }

    private void OnThemeChanged()
    {
        backgroundInitialColor = background.color;
        textInitialColor = label.color;
    }

    private void UpdateLabelIfNeeded(string precedingCharacter)
    {
        if(!IsDiacriticizer) return;

        string newLabel = "";

        if(!string.IsNullOrEmpty(precedingCharacter))
        {
            string lastAlternateGlyphSelected = diacriticsCustomization.GetLastAlternateGlyphSelectedForCurrentPrecedingCharacter(IsLeftDiacriticizer);
            if(!string.IsNullOrEmpty(lastAlternateGlyphSelected))
            {
                newLabel = lastAlternateGlyphSelected;
            }
        }
        SetLabel(newLabel);
    }

    private void CalculateDirection()
    {
        float x = transform.localPosition.x;
        float y = transform.localPosition.y;
        float t = 6.0f;

        if (MathUtils.ApproximatelyEquals(x,0,t) && MathUtils.ApproximatelyEquals(y,0,t)) cardinalDirection = CardinalDirection.Center;
        else if (MathUtils.ApproximatelyEquals(x,0,t) && y > x) cardinalDirection = CardinalDirection.North;
        else if (MathUtils.ApproximatelyEquals(x,0,t) && x > y) cardinalDirection = CardinalDirection.South;
        else if (MathUtils.ApproximatelyEquals(y,0,t) && y > x) cardinalDirection = CardinalDirection.West;
        else if (MathUtils.ApproximatelyEquals(y,0,t) && x > y) cardinalDirection = CardinalDirection.East;
        else if (x > 0 && x > y) cardinalDirection = CardinalDirection.SouthEast;
        else if (x > 0 && x < y) cardinalDirection = CardinalDirection.NorthEast;
        else if (x < 0 && x < y) cardinalDirection = CardinalDirection.NorthWest;
        else if (x < 0 && x > y) cardinalDirection = CardinalDirection.SouthWest;

        directionCalculated = true;
    }

    private void OnShiftStateChanged(ShiftButton source, ShiftState state)
    {
        if(!FollowShiftStateForCase) return;
        isUppercase = state != ShiftState.notPressed;
        SetCase(isUppercase);
    }

    private void SetCase(bool upper)
    {
        char[] chars = GetLabel().ToCharArray();
        if (chars.Length == 1 && char.IsLetter(chars[0]))
        {
            SetLabel(upper ? GetLabel().ToUpper(): GetLabel().ToLower());
        }
    }

    private void SetLabel(string l)
    {
        label.text = l;
        defaultLable = label.text;
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

    public void SetHilight(bool hilight, bool useAlsoBackground = true, bool showPreviewPopup = true)
    {
        if(hilight)
        {
            if (showPreviewPopup) useAlsoBackground = false;
            if (useAlsoBackground) background.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.GLYPH_SELECTED_BACKGROUND);

            label.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.GLYPH_SELECTED);

            if (showPreviewPopup)
            {
                if (isOnVeryTopRow && (settingsManager.TopBarHidden|| settingsManager.TopBarWentBottom))
                {
                    if (previewPopupGameObject == null)
                    {
                        previewPopupGameObject = Instantiate(previewPopupSmallPrefab);
                    }
                }

                if (previewPopupGameObject == null)
                {
                    previewPopupGameObject = Instantiate(previewPopupPrefab);
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
                Destroy(previewPopupGameObject);
                previewPopupGameObject = null;
            }
        }
    }
}
