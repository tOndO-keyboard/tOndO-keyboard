using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

[RequireComponent(typeof(Button))]
public abstract class AnalogStick : MonoBehaviour, IPointerDownHandler, IPointerUpHandler
{
    [SerializeField]
    protected CardinalDirection[] validCardinalDirection;

    protected NativeInterface nativeInterface;
    protected RectTransform rectTransform;
    protected Transform contentTransform;

    protected Vector2 firstPressPos;
    protected Vector2 secondPressPos;

    protected Vector3 initialPosition;

    protected int touchIndex = 0;

    protected CardinalDirection selectedDirection;

    protected bool firstDirectionSelected = false;

    private float lerpButtonTime = 0;
    private float lerpButtonDuration = 0.1f;
    private bool lerpAnalogStickInProgress = false;

    private bool awakeDone = false;

    public virtual bool Selected { get; protected set; }


    protected virtual void Awake() {
        nativeInterface = NativeInterface.Instance;
        rectTransform = GetComponent<RectTransform>();
        contentTransform = transform.GetChild(0).GetComponent<Transform>();
        initialPosition = contentTransform.localPosition;
        awakeDone = true;
    }

    protected virtual void Deselect() {
        Selected = false;
        StartLerpAnalogStick();
    }

    protected abstract void SubmitSelected();

    protected void LerpAnalogStickToInitialLocation() {
        if (!awakeDone) return;
        if (!lerpAnalogStickInProgress) return;
        lerpButtonTime += Time.deltaTime;
        contentTransform.localPosition = Vector3.Lerp(contentTransform.localPosition, initialPosition, lerpButtonTime / lerpButtonDuration);
        if (Vector3.Distance(contentTransform.localPosition, initialPosition) < 0.01f) {
            StopLerpAnalogStick();
        }
    }

    protected void StartLerpAnalogStick() {
        lerpAnalogStickInProgress = true;
        lerpButtonTime = 0;
    }

    protected void StopLerpAnalogStick() {
        lerpAnalogStickInProgress = false;
        lerpButtonTime = 0;
    }

    public virtual void OnPointerDown(PointerEventData eventData)
    {
        Selected = true;

        nativeInterface.Vibrate();
        touchIndex = Input.touches.Length - 1;
        firstDirectionSelected = true;
    }

    public virtual void OnPointerUp(PointerEventData eventData)
    {
        SubmitSelected();
        Selected = false;
    }
}
