using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class SymbolsButton : KeyboardButton
{
    [SerializeField]
    private GameObject mainButtonsRoot;

    [SerializeField]
    private GameObject symbolsButtonsRoot;

    [SerializeField]
    private GameObject symbols2ButtonsRoot;

    [SerializeField]
    private GameObject[] shiftButtons;

    [SerializeField]
    private Image icon;

    [SerializeField]
    private Sprite symbolsSprite;

    [SerializeField]
    private Sprite lettersSprite;

    [SerializeField]
    private SymbolsButton twin;

    private bool currentStateSymbols = false;

    private void Awake()
    {
        TondoAnalogStick.GlyphCommitted += OnGlyphCommitted;
        NativeInterface.InputViewStarted += OnInputViewStarted;
        NativeInterface.SetInputType += OnSetInputType;
    }

    private void OnInputViewStarted(string precedingCharacter)
    {
        OnGlyphCommitted(" ");
    }

    private void OnSetInputType(EditorInputType type)
    {
        currentStateSymbols = (type & EditorInputType.Numerical) > 0;
        switchState();
    }

    private void OnGlyphCommitted(string selectedGlyph)
    {
        if (string.IsNullOrEmpty(selectedGlyph)) return;

        if (selectedGlyph.Equals(" "))
        {
            currentStateSymbols = false;
            switchState();
        }
    }
    public override void OnKeyTrigger()
    {
        currentStateSymbols = !currentStateSymbols;
        switchState();
    }

    private void switchState()
    {
        symbolsButtonsRoot.SetActive(currentStateSymbols);
        symbols2ButtonsRoot.SetActive(false);
        foreach (GameObject shiftButton in shiftButtons)
        {
            shiftButton.SetActive(!currentStateSymbols);
        }
        mainButtonsRoot.SetActive(!currentStateSymbols);
        icon.sprite = currentStateSymbols ? lettersSprite : symbolsSprite;
        twin?.SetState(currentStateSymbols);
    }

    public void SetState(bool state)
    {
        currentStateSymbols = state;
        icon.sprite = currentStateSymbols ? lettersSprite : symbolsSprite;
    }

}
