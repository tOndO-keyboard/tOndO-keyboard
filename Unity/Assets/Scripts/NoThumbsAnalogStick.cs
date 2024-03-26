
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class NoThumbsAnalogStick : MonoBehaviour
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

    private NativeInterface nativeInterface;
    private RectTransform rectTransform;
    private Transform contentTransform;

    private Vector3 initialPosition;

    private bool firstDirectionSelected = false;

    private bool awakeDone = false;

    private void Update()
    {
        needleCenter.enabled = needle.rect.size.y > 0;

        float angle = 0;

        int cloveNumber = noThumbsGlyphs.Count;
        int cloveAmplitude = 360 / cloveNumber;

        /*if (SettingsManager.Instance.Reverse)
        {
            angle = Mathf.Atan2(firstPressPos.y - secondPressPos.y, firstPressPos.x - secondPressPos.x) * 180 / Mathf.PI;
        }
        else
        {
            angle = Mathf.Atan2(secondPressPos.y - firstPressPos.y, secondPressPos.x - firstPressPos.x) * 180 / Mathf.PI;
        }*/

        Vector3 acceleration = Input.acceleration;
        //acceleration = Quaternion.Euler(90, 0, 0) * acceleration;
        angle = Mathf.Atan2(acceleration.y, acceleration.x) * Mathf.Rad2Deg;
        angle = (angle + 360) % 360;

        //if (angle < 0) angle = 360 + angle;

        float distance = scale(-1, 0, 0, rectTransform.rect.size.x * 0.5f,- Mathf.Abs(acceleration.z));

        /*if (SettingsManager.Instance.Sticky)
        {
            float closestMultiple = MathUtils.GetClosestMultiple(angle, cloveAmplitude);
            float threshold = cloveAmplitude * 0.5f;
            if (Mathf.Abs(closestMultiple - angle) < threshold)
            {
                angle = MathUtils.SquircleAngle(angle, 3, closestMultiple, threshold);
            }
        }
        else if (SettingsManager.Instance.VerySticky)
        {*/
            angle = MathUtils.GetClosestMultiple(angle, cloveAmplitude);
        //}

        //if (SettingsManager.Instance.InputType == InputType.Joystick)
        //{
            float distanceJ = distance * 0.5f;
            distanceJ = distanceJ > rectTransform.rect.size.x * 0.06f ? rectTransform.rect.size.x * 0.06f : distanceJ;

            float angleRad = angle * Mathf.Deg2Rad;

            float deltaX = distanceJ * Mathf.Cos(angleRad);
            float deltaY = distanceJ * Mathf.Cos(90 * Mathf.Deg2Rad - angleRad);

            contentTransform.localPosition = new Vector3(initialPosition.x + deltaX, initialPosition.y + deltaY, initialPosition.z);

        //}
        //else if (SettingsManager.Instance.InputType == InputType.Needle)
        //{
            //needle.localEulerAngles = new Vector3(0, 0, angle - 90);
            //needle.sizeDelta = new Vector2(needle.rect.size.x, distance > rectTransform.rect.size.x * 0.5f ? rectTransform.rect.size.x * 0.5f : distance);
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

    private float scale(float OldMin, float OldMax, float NewMin, float NewMax, float OldValue)
    {

        float OldRange = (OldMax - OldMin);
        float NewRange = (NewMax - NewMin);
        float NewValue = (((OldValue - OldMin) * NewRange) / OldRange) + NewMin;

        return (NewValue);
    }


    private void HilightAllDirectionBut(int direction, bool useAlsoBackground = true)
    {
        foreach (NoThumbsGlyph glyph in noThumbsGlyphs.Values)
        {
            glyph.SetHilight(glyph.AngularIndex == direction, useAlsoBackground, SettingsManager.Instance.ShowPopupPreview);
        }
    }

    protected void Awake()
    {
        nativeInterface = NativeInterface.Instance;
        rectTransform = GetComponent<RectTransform>();
        contentTransform = transform.GetChild(0).GetComponent<Transform>();
        initialPosition = contentTransform.localPosition;

        contentTransform = transform.GetChild(0).GetComponent<Transform>();
        initialPosition = contentTransform.localPosition;

        RadialLayout radialLayout = GetComponentInChildren<RadialLayout>();

        NoThumbsGlyph[] noThumbsGlyphsArray = GetComponentsInChildren<NoThumbsGlyph>();
        noThumbsGlyphs = new Dictionary<int, NoThumbsGlyph>();
        foreach (NoThumbsGlyph b in noThumbsGlyphsArray)
        {
            noThumbsGlyphs.Add(b.AngularIndex, b);
        }

        awakeDone = true;
    }

    private void Deselect()
    {
        if (selectedAngularIndex == -1) return;

        selectedAngularIndex = -1;
        needle.sizeDelta = new Vector2(needle.rect.size.x, 0);
        HilightAllDirectionBut(-1);
    }

    private void SubmitSelected()
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
}