using TMPro;
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

