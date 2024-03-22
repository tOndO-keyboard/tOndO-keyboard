
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class NoThumbsAnalogStick : AnalogStick
{
    [SerializeField]
    private RectTransform needle;

    [SerializeField]
    private Image needleCenter;

    private Dictionary<int, NoThumbsGlyph> noThumbsGlyphs;

    public delegate void GlyphCommittedEventDelegate(string selectedGlyph);
    public static event GlyphCommittedEventDelegate GlyphCommitted;

    public delegate void GlyphCommittingEventDelegate(string precedingGlyph, string selectedGlyph);
    public static event GlyphCommittingEventDelegate GlyphCommitting;

    private int selectedAngularIndex = -1;
    private int previousselectedAngularIndex = -1;

    private void Update()
    {
        needleCenter.enabled = needle.rect.size.y > 0;

#if UNITY_EDITOR
        if (InputHelper.GetTouches().Count > 0 && Selected)
        {
            Touch touch = InputHelper.GetTouches()[0];
#else
        if(InputHelper.GetTouches().Count > 0 && InputHelper.GetTouches().Count <= touchIndex)
        {
            touchIndex = Input.touches.Length - 1;
        }

        if (Input.touches.Length > 0 && Input.touches.Length > touchIndex && Selected)
        {
            Touch touch = Input.GetTouch(touchIndex);
#endif
            if (touch.phase == TouchPhase.Began)
            {
                firstPressPos = new Vector2(touch.position.x, touch.position.y);
            }
            else if (touch.phase == TouchPhase.Moved)
            {
                secondPressPos = new Vector2(touch.position.x, touch.position.y);

                float angle = 0;

                int cloveNumber = noThumbsGlyphs.Count;
                int cloveAmplitude = 360 / cloveNumber;

                if (SettingsManager.Instance.Reverse)
                {
                    angle = Mathf.Atan2(firstPressPos.y - secondPressPos.y, firstPressPos.x - secondPressPos.x) * 180 / Mathf.PI;
                }
                else
                {
                    angle = Mathf.Atan2(secondPressPos.y - firstPressPos.y, secondPressPos.x - firstPressPos.x) * 180 / Mathf.PI;
                }

                if (angle < 0) angle = 360 + angle;

                float distance = Vector2.Distance(secondPressPos, firstPressPos);

                if (SettingsManager.Instance.Sticky)
                {
                    float closestMultiple = MathUtils.GetClosestMultiple(angle, cloveAmplitude);
                    float threshold = cloveAmplitude * 0.5f;
                    if (Mathf.Abs(closestMultiple - angle) < threshold)
                    {
                        angle = MathUtils.SquircleAngle(angle, 3, closestMultiple, threshold);
                    }
                }
                else if (SettingsManager.Instance.VerySticky)
                {
                    angle = MathUtils.GetClosestMultiple(angle, cloveAmplitude);
                }

                //if (SettingsManager.Instance.InputType == InputType.Joystick)
                //{
                    float distanceJ = distance * 0.5f;
                    distanceJ = distanceJ > rectTransform.rect.size.x * 0.06f ? rectTransform.rect.size.x * 0.06f : distanceJ;

                    float angleRad = angle * Mathf.Deg2Rad;

                    float deltaX = distanceJ * Mathf.Cos(angleRad);
                    float deltaY = distanceJ * Mathf.Cos(90 * Mathf.Deg2Rad - angleRad);

                    StopLerpAnalogStick();
                    contentTransform.localPosition = new Vector3(initialPosition.x + deltaX, initialPosition.y + deltaY, initialPosition.z);

                //}
                //else if (SettingsManager.Instance.InputType == InputType.Needle)
                //{
                    needle.localEulerAngles = new Vector3(0, 0, angle - 90);
                    needle.sizeDelta = new Vector2(needle.rect.size.x, distance > rectTransform.rect.size.x * 0.5f ? rectTransform.rect.size.x * 0.5f : distance);
                //}

                int newselectedAngularIndex = -1;

                angle = 360 - angle;
                float preciseCloveAmplitude = 360 / cloveNumber;

                if (distance > rectTransform.rect.size.x * (SettingsManager.Instance.ExternalGlyphSelectionDistanceFactor * 0.01f))
                {
                    newselectedAngularIndex = 0;

                    float firstDelta = -(preciseCloveAmplitude / 2);

                    for (int i = 0; i < cloveNumber; i++)
                    {
                        if (angle >= firstDelta + preciseCloveAmplitude * i && angle < firstDelta + preciseCloveAmplitude * (i + 1)) newselectedAngularIndex = i;
                    }
                }

                if (!firstDirectionSelected && previousselectedAngularIndex != selectedAngularIndex)
                {
                    nativeInterface.Vibrate();
                }
                previousselectedAngularIndex = selectedAngularIndex;
                if (selectedAngularIndex != -1 && selectedAngularIndex != 0)
                {
                    firstDirectionSelected = false;
                }

                selectedAngularIndex = newselectedAngularIndex;

                HilightAllDirectionBut(newselectedAngularIndex, !(SettingsManager.Instance.Sticky && (SettingsManager.Instance.InputType == InputType.Needle || SettingsManager.Instance.InputType == InputType.DoubleNeedle)));
            }
            else if (touch.phase == TouchPhase.Ended || touch.phase == TouchPhase.Canceled)
            {
                SubmitSelected();
            }
        }
        else if (InputHelper.GetTouches().Count == 0)
        {
            Deselect();
        }

        LerpAnalogStickToInitialLocation();
    }

    private void HilightAllDirectionBut(int direction, bool useAlsoBackground = true)
    {
        foreach (NoThumbsGlyph glyph in noThumbsGlyphs.Values)
        {
            glyph.SetHilight(glyph.AngularIndex == direction, useAlsoBackground, SettingsManager.Instance.ShowPopupPreview);
        }
    }

    protected override void Awake()
    {
        base.Awake();
        RadialLayout radialLayout = GetComponentInChildren<RadialLayout>();

        NoThumbsGlyph[] noThumbsGlyphsArray = GetComponentsInChildren<NoThumbsGlyph>();
        noThumbsGlyphs = new Dictionary<int, NoThumbsGlyph>();
        foreach (NoThumbsGlyph b in noThumbsGlyphsArray)
        {
            noThumbsGlyphs.Add(b.AngularIndex, b);
        }
    }

    protected override void Deselect()
    {
        if (selectedAngularIndex == -1) return;

        base.Deselect();
        selectedAngularIndex = -1;
        needle.sizeDelta = new Vector2(needle.rect.size.x, 0);
        HilightAllDirectionBut(-1);
    }

    protected override void SubmitSelected()
    {
        if (selectedAngularIndex != -1)
        {
            string selectedString = "";
            NoThumbsGlyph selectedGlyph = noThumbsGlyphs[selectedAngularIndex];
            selectedString = selectedGlyph.GetLabel();

            if (!string.IsNullOrEmpty(selectedString))
            {
                GlyphCommitting?.Invoke(nativeInterface.GetPrecedingCharacter(1), selectedString);

                nativeInterface.CommitString(selectedString);
                
                GlyphCommitted?.Invoke(selectedString);
            }
        }

        Deselect();
    }

    public override void OnPointerDown(PointerEventData eventData)
    {
        base.OnPointerDown(eventData);
        selectedAngularIndex = 0;
        HilightAllDirectionBut(0);
    }
}