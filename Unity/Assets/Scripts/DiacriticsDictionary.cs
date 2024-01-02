using UnityEngine;
using System;
using System.Collections.Generic;

[Serializable]
public class DiacriticsDictionary : SDictionary<string, DiacriticsDictionaryEntry>
{
    [SerializeField]
    private List<string> serializedKeys;

    protected override List<string> SerializedKeys
    {
        get { return serializedKeys; }
        set { serializedKeys = value; }
    }

    [SerializeField]
    private List<DiacriticsDictionaryEntry> serializedValues;

    protected override List<DiacriticsDictionaryEntry> SerializedValues
    {
        get { return serializedValues; }
        set { serializedValues = value; }
    }
}

[Serializable]
public class DiacriticsDictionaryEntry
{
    public string[] LeftGlyphs;

    [HideInInspector]
    public int LastLeftGlyphSelectedIndex;

    public string[] RightGlyphs;

    [HideInInspector]
    public int LastRightGlyphSelectedIndex;
}