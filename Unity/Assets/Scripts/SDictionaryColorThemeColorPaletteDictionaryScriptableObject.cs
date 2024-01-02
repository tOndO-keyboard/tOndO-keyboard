using System;
using System.Collections.Generic;
using UnityEngine;

[Serializable]
public class SDictionaryColorThemeColorPaletteDictionaryScriptableObject : SDictionary<COLOR_THEME, ColorPaletteDictionaryScriptableObject>
{
    [SerializeField]
    private List<COLOR_THEME> serializedKeys;

    protected override List<COLOR_THEME> SerializedKeys
    {
        get { return serializedKeys; }
        set { serializedKeys = value; }
    }

    [SerializeField]
    private List<ColorPaletteDictionaryScriptableObject> serializedValues;

    protected override List<ColorPaletteDictionaryScriptableObject> SerializedValues
    {
        get { return serializedValues; }
        set { serializedValues = value; }
    }
}
