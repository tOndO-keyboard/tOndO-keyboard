using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class TondoAnalogStick : AnalogStick
{
    [SerializeField]
    private AnalogStickPosition analogStickPosition;
    public AnalogStickPosition AnalogStickPosition
    {
        get
        {
            return analogStickPosition;
        }
    }

    [SerializeField]
    private RectTransform doubleNeedle;

    [SerializeField]
    private RectTransform needle;

    [SerializeField]
    private Image needleCenter;

    private float longPressTimeSpan = 0.25f;

    private DiacriticsCustomization diacriticsCustomization;

    private Dictionary<CardinalDirection, TondoGlyph> tondoGlyphs; 
    
    public delegate void GlyphCommittedEventDelegate(string selectedGlyph);
    public static event GlyphCommittedEventDelegate GlyphCommitted;

    public delegate void GlyphCommittingEventDelegate(string precedingGlyph, string selectedGlyph);
    public static event GlyphCommittingEventDelegate GlyphCommitting;

    private bool isLongPressed = false;
    private float longPressTimer = 0.0f;
    private CardinalDirection longPressPreviousSelectedDirection = CardinalDirection.Null;
    private CardinalDirection previousSelectedDirection = CardinalDirection.Null;
    private bool glyphsWasReplacedWithAlternatives = false;
    private string[] lastUsedAlternateGlyphs;
    private bool lastIsLeft;

    private List<CardinalDirection> sortedDirections;

    private void FixedUpdate()
    {
        CheckLongPress();
    }

    private void CheckLongPress()
    {
        if(longPressPreviousSelectedDirection != CardinalDirection.Null && longPressPreviousSelectedDirection == selectedDirection)
        {
            longPressTimer += Time.fixedDeltaTime;
            UpdateIsLongPressedState(longPressTimer > longPressTimeSpan);
        }
        else
        {
            longPressTimer = 0.0f;
            UpdateIsLongPressedState(false);
        }

        longPressPreviousSelectedDirection = selectedDirection;
    }

    private void UpdateIsLongPressedState(bool value)
    {
        if(isLongPressed == value) return;
        isLongPressed = value;
        if (isLongPressed)
        {
            firstDirectionSelected = false;
        }
        ReplaceGlyphsWithAlternativesIfNeeded();
    }

    private void SortDirectionsIfNeeded(bool left)
    {
        if(sortedDirections != null)
        {
            return;
        }

        sortedDirections = new List<CardinalDirection>(tondoGlyphs.Keys);
        CardinalDirectionSorter sorter = new CardinalDirectionSorter(CardinalDirection.North, left);
        sortedDirections.Sort(sorter.CircularCompare);
    }

    private void ReplaceGlyphsWithAlternativesIfNeeded()
    {
        if(!isLongPressed || glyphsWasReplacedWithAlternatives)  return;
        
        if(selectedDirection == CardinalDirection.Null)  return;

        TondoGlyph selectedGlyph = tondoGlyphs[selectedDirection];
        if(!selectedGlyph.IsDiacriticizer)  return;

        SortDirectionsIfNeeded(selectedGlyph.IsLeftDiacriticizer);

        string[] alternateGlyphs = diacriticsCustomization.GetAlternateGlyphsForCurrentPrecedingCharacter(selectedGlyph.IsLeftDiacriticizer);

        if(alternateGlyphs == null || alternateGlyphs.Length <= 0) return;

        int index = 0;

        foreach(CardinalDirection direction in sortedDirections)
        {
            if(!tondoGlyphs[direction].IsDiacriticizer)
            {
                string tempLabel = "";
                if(alternateGlyphs.Length > index) tempLabel = alternateGlyphs[index];
                tondoGlyphs[direction].SetTemporaryLabel(tempLabel);
                
                index++;
            }
        }

        lastUsedAlternateGlyphs = alternateGlyphs;
        lastIsLeft = selectedGlyph.IsLeftDiacriticizer;

        glyphsWasReplacedWithAlternatives = true;
    }

    private void ResetGlyphsIfNeeded()
    {
        if(!glyphsWasReplacedWithAlternatives) return;

        foreach(KeyValuePair<CardinalDirection, TondoGlyph> kvp in tondoGlyphs)
        {
            if(!kvp.Value.IsDiacriticizer)
            {
                kvp.Value.ResetTemporaryLabel();
            }
        }

        glyphsWasReplacedWithAlternatives = false;
    }

    private void Update()
    {
        needleCenter.enabled = needle.rect.size.y > 0 || doubleNeedle.rect.size.y > 0;

#if UNITY_EDITOR
        if(InputHelper.GetTouches().Count > 0 && Selected)
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

                int cloveNumber = validCardinalDirection.Length;
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

                if(SettingsManager.Instance.Sticky)
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
                
                if (SettingsManager.Instance.InputType == InputType.Joystick)
                {
                    float distanceJ = distance * 0.5f;
                    distanceJ = distanceJ > rectTransform.rect.size.x * 0.06f ? rectTransform.rect.size.x * 0.06f : distanceJ;

                    float angleRad = angle * Mathf.Deg2Rad;

                    float deltaX = distanceJ * Mathf.Cos(angleRad);
                    float deltaY = distanceJ * Mathf.Cos(90 * Mathf.Deg2Rad - angleRad);

                    StopLerpAnalogStick();
                    contentTransform.localPosition = new Vector3(initialPosition.x + deltaX, initialPosition.y + deltaY, initialPosition.z);

                }
                else if (SettingsManager.Instance.InputType == InputType.DoubleNeedle)
                {
                    float distanceD = distance * 2;
                    doubleNeedle.localEulerAngles = new Vector3(0, 0, angle - 90);
                    doubleNeedle.sizeDelta = new Vector2(needle.rect.size.x, distanceD > rectTransform.rect.size.x ? rectTransform.rect.size.x : distanceD);
                }
                else if (SettingsManager.Instance.InputType == InputType.Needle)
                {
                    needle.localEulerAngles = new Vector3(0, 0, angle - 90);
                    needle.sizeDelta = new Vector2(needle.rect.size.x, distance > rectTransform.rect.size.x * 0.5f ? rectTransform.rect.size.x * 0.5f : distance);
                }

                CardinalDirection newSelectedDirection = CardinalDirection.Center;

                if (distance > rectTransform.rect.size.x * (SettingsManager.Instance.ExternalGlyphSelectionDistanceFactor*0.01f))
                {
                    newSelectedDirection = validCardinalDirection[0];

                    float firstDelta = -(cloveAmplitude / 2);

                    for (int i = 0; i < cloveNumber; i++)
                    {
                        if (angle >= firstDelta + cloveAmplitude * i && angle < firstDelta + cloveAmplitude * (i + 1)) newSelectedDirection = validCardinalDirection[i];
                    }
                }

                if (!firstDirectionSelected && previousSelectedDirection != selectedDirection)
                {
                    nativeInterface.Vibrate();
                }
                previousSelectedDirection = selectedDirection;
                if (selectedDirection != CardinalDirection.Center && selectedDirection != CardinalDirection.Null)
                {
                    firstDirectionSelected = false;
                }

                selectedDirection = newSelectedDirection;

                HilightAllDirectionBut(newSelectedDirection, !(SettingsManager.Instance.Sticky && (SettingsManager.Instance.InputType == InputType.Needle || SettingsManager.Instance.InputType == InputType.DoubleNeedle)));
            }
            else if(touch.phase == TouchPhase.Ended || touch.phase == TouchPhase.Canceled)
            {
                SubmitSelected();
            }
        }
        else if(InputHelper.GetTouches().Count == 0)
        {
            Deselect();
        }

        LerpAnalogStickToInitialLocation();
    }

    private void HilightAllDirectionBut(CardinalDirection direction, bool useAlsoBackground = true)
    {
        foreach(TondoGlyph glyph in tondoGlyphs.Values)
        {
            glyph.SetHilight(glyph.CardinalDirection == direction, useAlsoBackground, SettingsManager.Instance.ShowPopupPreview);
        }
    }

    protected override void Awake()
    {
        base.Awake();
        RadialLayout radialLayout = GetComponentInChildren<RadialLayout>();
        if(radialLayout.enabled)
        {
            DebugLogger.Log("Please disable all Radial Layouts before entering play mode.", DebugLogger.LogType.WARNING);
            return;
        }

        TondoGlyph[] tondoGlyphsArray = GetComponentsInChildren<TondoGlyph>();
        tondoGlyphs = new Dictionary<CardinalDirection, TondoGlyph>();
        foreach(TondoGlyph b in tondoGlyphsArray)
        {
            tondoGlyphs.Add(b.CardinalDirection, b);
        }

        diacriticsCustomization = DiacriticsCustomization.Instance;
    }

    protected override void Deselect() 
    {
        if(selectedDirection == CardinalDirection.Null) return;

        base.Deselect();
        selectedDirection = CardinalDirection.Null;
        needle.sizeDelta = new Vector2(needle.rect.size.x, 0);
        doubleNeedle.sizeDelta = new Vector2(needle.rect.size.x, 0);
        HilightAllDirectionBut(CardinalDirection.Null);
        ResetGlyphsIfNeeded();
    }

    protected override void SubmitSelected()
    {
        if (selectedDirection != CardinalDirection.Null)
        {
            string selectedString = "";
            TondoGlyph selectedGlyph = tondoGlyphs[selectedDirection];
            if(selectedGlyph.IsDiacriticizer)
            {
                selectedString = diacriticsCustomization.GetLastAlternateGlyphSelectedForCurrentPrecedingCharacter(selectedGlyph.IsLeftDiacriticizer);
            }
            else
            {
                selectedString = selectedGlyph.GetLabel();
            }

            if(glyphsWasReplacedWithAlternatives)
            {
                int index = -1;
                for(int i = 0; i < lastUsedAlternateGlyphs.Length; i++)
                {
                    if(lastUsedAlternateGlyphs[i].Equals(selectedString))
                    {
                        index = i;
                    }
                }

                if(index >= 0)
                {
                    diacriticsCustomization.SetLastGlyphSelectedIndexForCurrentPrecedingCharacter(index, lastIsLeft);
                }
            }

            if(!string.IsNullOrEmpty(selectedString))
            {
                GlyphCommitting?.Invoke(nativeInterface.GetPrecedingCharacter(1), selectedString);

                if(selectedGlyph.IsDiacriticizer || glyphsWasReplacedWithAlternatives)
                {
                    nativeInterface.ReplaceLastCharacterWith(selectedString);
                }
                else
                {
                    nativeInterface.CommitString(selectedString);
                }

                GlyphCommitted?.Invoke(selectedString);
            }
        }

        Deselect();
    }

    public override void OnPointerDown(PointerEventData eventData)
    {
        base.OnPointerDown(eventData);
        selectedDirection = CardinalDirection.Center;
        HilightAllDirectionBut(CardinalDirection.Center);
    }
}