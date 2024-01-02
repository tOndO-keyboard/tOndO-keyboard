using System;
using System.Collections.Generic;
using UnityEngine;

[Serializable]
public class SDictionaryPaletteElementColor : SDictionary<PALETTE_ELEMENT, Color>
{
    [SerializeField]
    private List<PALETTE_ELEMENT> serializedKeys;

    protected override List<PALETTE_ELEMENT> SerializedKeys
    {
        get { return serializedKeys; }
        set { serializedKeys = value; }
    }

    [SerializeField]
    private List<Color> serializedValues;

    protected override List<Color> SerializedValues
    {
        get { return serializedValues; }
        set { serializedValues = value; }
    }
}
