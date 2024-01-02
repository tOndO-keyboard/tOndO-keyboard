using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class ShiftButton : KeyboardButton
{
    public enum ShiftState { notPressed, pressed, doublePressed };

    public delegate void ShiftChangedStateEventDelegate(ShiftButton source, ShiftState newState);

    public static event ShiftChangedStateEventDelegate ShiftChangedState;

    [SerializeField]
    private Image icon;

    [SerializeField]
    private Sprite notPressedSprite;

    [SerializeField]
    private Sprite doublePressedSprite;

    [SerializeField]
    private float doublePressionDelay = 0.5f;

    private EditorInputType inputType = EditorInputType.Text;
    private ShiftState currentState = ShiftState.pressed;

    private float lastShiftPressedTime = 0;

    private bool setPressedAfterNextCommit = false;

    private void Awake()
    {
        ShiftButton.ShiftChangedState += OnShiftStateChanged;
        TondoAnalogStick.GlyphCommitted += OnGlyphCommitted;
        TondoAnalogStick.GlyphCommitting += OnGlyphCommitting;
        NativeInterface.InputViewStarted += OnInputViewStarted;
        NativeInterface.SetInputType += OnSetInputType;
        NativeInterface.CursorPositionChanged += OnCursorPositionChanged;
        DoneButton.DoneCommitted += OnDoneCommitted;
    }

    private void Start()
    {
        if (SettingsManager.Instance.DisableAutomaticCapitalization)
        {
            SetState(ShiftState.notPressed);
        }
    }

    private void OnSetInputType(EditorInputType type)
    {
        inputType = type;
        if (SettingsManager.Instance.DisableAutomaticCapitalization) return;
        if ((type & EditorInputType.Password) > 0) SetState(ShiftState.notPressed);
    }

    private void OnDoneCommitted(DoneButton source, DoneButton.DONE_ACTION_FLAG flag)
    {
        if (SettingsManager.Instance.DisableAutomaticCapitalization) return;

        if (currentState == ShiftState.notPressed && flag == DoneButton.DONE_ACTION_FLAG.NEWLINE)
        {
            SetState(ShiftState.pressed);
        }
    }

    private void OnInputViewStarted(string precedingCharacter)
    {
        if (SettingsManager.Instance.DisableAutomaticCapitalization) return;

        if (string.IsNullOrEmpty(precedingCharacter))
        {
            SetState(ShiftState.pressed);
        }
        else
        {
            SetState(ShiftState.notPressed);
        }
    }

    private void OnCursorPositionChanged(string precedingCharacter)
    {
        if (SettingsManager.Instance.DisableAutomaticCapitalization) return;

        if (currentState == ShiftState.notPressed && 
            string.IsNullOrEmpty(precedingCharacter) && 
            (inputType & EditorInputType.Password) == 0)
        {
            SetState(ShiftState.pressed);
        }
    }

    private void OnGlyphCommitted(string selectedGlyph)
    {
        if (currentState == ShiftState.pressed)
        {
            currentState = ShiftState.notPressed;
            icon.sprite = notPressedSprite;
            icon.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.PERMANENT_BUTTON_ICON);
            ShiftChangedState?.Invoke(this, currentState);
        }


        if (SettingsManager.Instance.DisableAutomaticCapitalization) return;

        if (setPressedAfterNextCommit)
        {
            setPressedAfterNextCommit = false;
            SetState(ShiftState.pressed);
        }
    }

    private void OnGlyphCommitting(string precedingCharacter, string selectedGlyph)
    {
        if(string.IsNullOrEmpty(precedingCharacter) || string.IsNullOrEmpty(selectedGlyph)) return;

        if (SettingsManager.Instance.DisableAutomaticCapitalization) return;

        if(currentState == ShiftState.notPressed && 
            (precedingCharacter.Equals(".") ||
            precedingCharacter.Equals("?") ||
            precedingCharacter.Equals("!") ||
            precedingCharacter.Equals("¿") ||
            precedingCharacter.Equals("¡") ||
            precedingCharacter.Equals("‽")) && 
            (selectedGlyph.Equals(" ")||
            selectedGlyph.Equals(Environment.NewLine)))
        {
            setPressedAfterNextCommit = true;
        }
    }

    private void OnShiftStateChanged(ShiftButton source, ShiftState state)
    {
        if (source == this) return;
        SetState(state);
    }

    private void SetState(ShiftState state)
    {
        if(currentState == state) return;

        currentState = state;
        switch (state)
        {
            case (ShiftState.notPressed):
                icon.sprite = notPressedSprite;
                icon.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.PERMANENT_BUTTON_ICON);
                break;
            case ShiftState.pressed:
                icon.sprite = notPressedSprite;
                icon.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.GLYPH_SELECTED);
                break;
            case ShiftState.doublePressed:
                icon.sprite = doublePressedSprite;
                icon.color = ColorThemeDictionaryReference.Instance.GetColorForCurrentTheme(PALETTE_ELEMENT.GLYPH_SELECTED);
                break;
        }

        ShiftChangedState?.Invoke(this, currentState);
    }

    public override void OnKeyTrigger()
    {
        if(Time.time - lastShiftPressedTime < doublePressionDelay)
        {
            SetState(ShiftState.doublePressed);
            lastShiftPressedTime = 0;
            return;
        }

        lastShiftPressedTime = Time.time;

        switch(currentState)
        {
            case ShiftState.notPressed:
            SetState(ShiftState.pressed);
            break;

            case ShiftState.pressed:
            SetState(ShiftState.notPressed);
            break;

            case ShiftState.doublePressed:
            SetState(ShiftState.notPressed);
            break;

        }
    }
}
