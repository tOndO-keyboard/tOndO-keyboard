using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

[RequireComponent(typeof(Button))]
public abstract class KeyboardButton : MonoBehaviour, IPointerDownHandler, IPointerUpHandler, IPointerExitHandler
{
    [SerializeField]
    protected bool deselectOnPointerExit = false;

    [SerializeField]
    protected bool triggerOnPointerDown = false;

    [SerializeField]
    private bool registerLongPress = false;

    [SerializeField, Min(0)]
    private float longPressTime = .5f;

    private float _longPressTimer = 0;
    private Button _button;
    private NativeInterface _nativeInterface;
    private ProLock _lock;
    private bool _lockAttempt = false;

    protected bool inputCancelled = false;

    protected NativeInterface NativeInterface
    {
        get
        {
            if(_nativeInterface == null)
            {
                _nativeInterface = NativeInterface.Instance;
            }
            return _nativeInterface;
        }
    }

    protected Button Button
    {
        get
        {
            if(_button==null)
            {
                _button = GetComponent<Button>();
            }
            return _button;
        }
    }

    protected ProLock Lock
    {
        get
        {
            if (!_lockAttempt)
            {
                _lock = GetComponent<ProLock>();
                _lockAttempt = true;
            }
            return _lock;
        }
    }

    public bool IsInteractable
    {
        get =>Button.interactable;
        set 
        {
            Button.interactable = value;
            enabled = value;
        }
    }

    public System.Action ClickOverride { get; set; }

    private void InternalOnKeyTrigger()
    {
        if (ClickOverride != null) ClickOverride();
        else OnKeyTrigger();
    }

    protected virtual void Update()
    {
        if (_longPressTimer == 0) return;
        _longPressTimer -= Time.unscaledDeltaTime;
        _longPressTimer = Mathf.Max(0, _longPressTimer);
        if (_longPressTimer == 0 && !inputCancelled) 
        {
            NativeInterface.Vibrate();
            OnLongPressTrigger();
        }
    }

    public void SetActive(bool active)
    {
        if (Lock != null) Lock.SetActive(active);
        else gameObject.SetActive(active);
    }
    
    public virtual void OnPointerDown(PointerEventData eventData)
    {
        inputCancelled = false;
        _longPressTimer = registerLongPress ? longPressTime : 0;
        if (triggerOnPointerDown)
        {
            NativeInterface.Vibrate();
            InternalOnKeyTrigger();
        }
    }

    public virtual void OnPointerUp(PointerEventData eventData)
    {
        //No pointer up if long press happened.
        if (registerLongPress && _longPressTimer == 0) return;
        //No pointer up if triggers with pointer down or input was cancelled.
        if (triggerOnPointerDown || inputCancelled) 
        {
            _longPressTimer = 0;
            return;
        }

        _longPressTimer = 0;
        NativeInterface.Vibrate();
        InternalOnKeyTrigger();
    }

    public virtual void OnPointerExit(PointerEventData eventData)
    {
        if (deselectOnPointerExit && !triggerOnPointerDown)
        {
            inputCancelled = true;
            Button.OnPointerUp(new PointerEventData(EventSystem.current));
        }
    }

    public abstract void OnKeyTrigger();

    public virtual void OnLongPressTrigger() { }

    public virtual void HandleProState(bool isPro) { }
}
