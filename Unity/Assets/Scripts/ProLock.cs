using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(KeyboardButton))]
public class ProLock : MonoBehaviour
{
    public enum LockType { Hide, Disable, OverrideClick, ButtonSpecific }
    [SerializeField]
    private bool forceDisable = false;
    public bool ForceDisable
    {
        set
        {
            forceDisable = value;
            SetPro(true);
        }
    }

    [SerializeField]
    private KeyboardButton _button = null;

    [Space]
    [SerializeField]
    private LockType _type = LockType.Disable;
    [SerializeField]
    private LockOverride _override = null;

    [Space]
    [SerializeField]
    private bool _useLockColor = false;
    [SerializeField]
    private Graphic _graphicToColor = null;
    [SerializeField]
    private Color _lockColor = Color.white;
    [SerializeField]
    private GameObject _lockObject = null;

    private Color _originalColor;
    private bool _isPro;

    private PaletteElementColorApplier _paletteElementColorApplier;
    private PALETTE_ELEMENT _originalPaletteElementColorElement = PALETTE_ELEMENT.NONE;

    public LockType Lock => _type;
    public KeyboardButton Button => _button == null ? (_button = GetComponent<KeyboardButton>()) : _button;

    private void Awake()
    {
        if (_useLockColor)
        {
            if (_graphicToColor == null)
            {
                DebugLogger.Log("ProLock useLockColor without a graphic element to color", DebugLogger.LogType.ERROR);
            }
            else
            {
                _paletteElementColorApplier = _graphicToColor.GetComponent<PaletteElementColorApplier>();
                if(_paletteElementColorApplier != null)
                {
                    _originalPaletteElementColorElement = _paletteElementColorApplier.PaletteElement;
                }
                _originalColor = _graphicToColor.color;
            }
        }
        NativeInterface.ProStateChange += SetPro;
        SetPro(NativeInterface.Instance.IsPro());
    }

    public bool CouldBeEnabled()
    {
        if (_isPro) return true;
        return _type != LockType.Hide;
    }

    private void SetPro(bool isPro)
    {
        if (forceDisable) isPro = true;

        _isPro = isPro;

        if (_type == LockType.Hide) gameObject.SetActive(isPro);
        else if (_type == LockType.Disable) Button.IsInteractable = isPro;
        else if (_type == LockType.OverrideClick)
        {
            if (_override == null) 
            {
                DebugLogger.Log("Override ProLock without an override function", DebugLogger.LogType.WARNING);
                Button.ClickOverride = null;
            } else Button.ClickOverride = !isPro ? _override.LockFunction : (System.Action)null;
        }
        else Button.HandleProState(isPro);

        if (_useLockColor)
        {
            if(_paletteElementColorApplier != null)
            {
                _paletteElementColorApplier.PaletteElement = isPro ? _originalPaletteElementColorElement : PALETTE_ELEMENT.NONE;
                _paletteElementColorApplier.OnThemeChanged();
            }
            _graphicToColor.color = isPro ? _originalColor : _lockColor;
        }

        if (_lockObject != null) _lockObject.SetActive(!isPro);
    }

    public void SetActive(bool active)
    {
        if (_type == LockType.Hide && !_isPro) return;
        gameObject.SetActive(active);
    }

#if UNITY_EDITOR
    private void OnValidate()
    {
        if (_button == null) _button = GetComponent<KeyboardButton>();
    }
#endif
}
