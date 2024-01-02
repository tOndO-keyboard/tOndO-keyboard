using UnityEngine;
using System;
using System.Collections.Generic;

[CreateAssetMenu(fileName = "Data", menuName = "ScriptableObjects/ColorPaletteDictionaryScriptableObject")]
public class ColorPaletteDictionaryScriptableObject : ScriptableObject
{
    [SerializeField]
    public SDictionaryPaletteElementColor ColorPaletteDictionary;
}