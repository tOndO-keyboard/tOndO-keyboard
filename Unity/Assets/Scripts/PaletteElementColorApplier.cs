using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(Graphic))]
public class PaletteElementColorApplier : MonoBehaviour
{
    public delegate void GraphicColorChangedEventDelegate();
    public event GraphicColorChangedEventDelegate GraphicColorChanged;

    [SerializeField]
    private PALETTE_ELEMENT paletteElement = PALETTE_ELEMENT.NONE;

    public PALETTE_ELEMENT PaletteElement
    {
        get { return paletteElement; }
        set { paletteElement = value; }
    }

    private ColorThemeDictionaryReference _colorThemeDictionaryReference;
    private ColorThemeDictionaryReference ColorThemeDictionaryReference
    {
        get
        {
            if(_colorThemeDictionaryReference == null)
            {
                _colorThemeDictionaryReference = ColorThemeDictionaryReference.Instance;
            }
            return _colorThemeDictionaryReference;
        }
    }

    private Graphic _graphic;
    private Graphic Graphic
    {
        get
        {
            if(_graphic == null)
            {
                _graphic = GetComponent<Graphic>();
            }
            return _graphic;
        }
    }

    private void UpdateColor(ColorThemeDictionaryReference ctdr)
    {
        if (ctdr != null && paletteElement != PALETTE_ELEMENT.NONE)
        {
            var a = Graphic.color.a;
            var color = ctdr.GetColorForCurrentTheme(paletteElement);
            color.a = a;
            Graphic.color = color;
        }
    }

    private void OnValidate()
    {
        if (paletteElement != PALETTE_ELEMENT.NONE)
            UpdateColor(FindObjectOfType<ColorThemeDictionaryReference>());
    }

    private void Awake()
    {
        ColorThemeDictionaryReference.ThemeChanged += OnThemeChanged;
        OnThemeChanged();
    }

    private void OnDestroy()
    {
        ColorThemeDictionaryReference.ThemeChanged -= OnThemeChanged;
    }

    public void OnThemeChanged()
    {
        UpdateColor(ColorThemeDictionaryReference);
        GraphicColorChanged?.Invoke();
    }
}
