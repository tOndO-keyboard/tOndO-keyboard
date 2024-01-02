using UnityEngine;
using System;
using System.Collections.Generic;

[CreateAssetMenu(fileName = "Data", menuName = "ScriptableObjects/ColorThemeDictionaryScriptableObject")]
public class ColorThemeDictionaryScriptableObject : ScriptableObject
{
    [SerializeField]
    public SDictionaryColorThemeColorPaletteDictionaryScriptableObject ColorThemeColorPaletteDictionary;
}