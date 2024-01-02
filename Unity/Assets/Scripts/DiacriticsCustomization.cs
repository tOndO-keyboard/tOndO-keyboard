using System;
using System.Collections.Generic;
using UnityEngine;

public class DiacriticsCustomization : LazySingleIstanceMonoBehaviour<DiacriticsCustomization>
{
    private DiacriticsDictionary diacriticsDictionary;
    public DiacriticsDictionary DiacriticsDictionary
    {
        get
        {
            deserialize();
            return diacriticsDictionary;
        }
    }

    private string lastDeserializedDiacriticsCustomizationString = String.Empty;

    private static readonly string playerPrefKeyPrefix = "DiacriticsDictionaryReferencePlayerPrefKey_";

    private NativeInterface nativeInterface;

    private void Awake()
    {
        nativeInterface = NativeInterface.Instance;
    }

    private void deserialize()
    {
        string diacriticsCustomizationString = SettingsManager.Instance.DiacriticsCustomizationString;

        if (diacriticsCustomizationString.Equals(lastDeserializedDiacriticsCustomizationString))
        {
            return;
        }

        lastDeserializedDiacriticsCustomizationString = diacriticsCustomizationString;

        string[] inputTextArray = diacriticsCustomizationString.Split(new[] { '\n' });

        if (inputTextArray != null && inputTextArray.Length > 0)
        {
            diacriticsDictionary = new DiacriticsDictionary();

            foreach (string line in inputTextArray)
            {
                string[] lineArray = line.Split(new[] { ' ' });

                string key = lineArray[0].ToLower();
                DiacriticsDictionaryEntry value = new DiacriticsDictionaryEntry();

                value.LastLeftGlyphSelectedIndex = 0;
                value.LastRightGlyphSelectedIndex = 0;

                if (lineArray.Length > 1)
                {
                    char[] leftChars = lineArray[1].ToCharArray();

                    string[] leftStrings = new string[leftChars.Length];
                    for (int i = 0; i < leftChars.Length; i++)
                    {
                        leftStrings[i] = char.ToString(leftChars[i]).ToLower();
                    }

                    value.LeftGlyphs = leftStrings;
                }
                if (lineArray.Length > 2)
                {
                    char[] rightChars = lineArray[2].ToCharArray();
                    string[] rightStrings = new string[rightChars.Length];
                    for (int i = 0; i < rightChars.Length; i++)
                    {
                        rightStrings[i] = char.ToString(rightChars[i]).ToLower();
                    }

                    value.RightGlyphs = rightStrings;
                }

                if (lineArray.Length > 3)
                {
                    int parsedValue = 0;
                    bool parsed = int.TryParse(lineArray[3], out parsedValue);
                    if(parsed) value.LastLeftGlyphSelectedIndex = parsedValue;
                }

                if (lineArray.Length > 4)
                {
                    int parsedValue = 0;
                    bool parsed = int.TryParse(lineArray[4], out parsedValue);
                    if (parsed) value.LastRightGlyphSelectedIndex = parsedValue;
                }

                if(!diacriticsDictionary.ContainsKey(key))
                {
                    diacriticsDictionary.Add(key, value);
                }
            }
        }
    }

    private void DeserializeLastGlyphsSelectedIndexes()
    {
        foreach(KeyValuePair<string, DiacriticsDictionaryEntry> kvp in DiacriticsDictionary)
        {
            string rightId = GetPlayerPrefsKeyForString(kvp.Key, false);
            string leftId = GetPlayerPrefsKeyForString(kvp.Key, true);
            kvp.Value.LastRightGlyphSelectedIndex = PlayerPrefs.GetInt(rightId, 0);
            kvp.Value.LastLeftGlyphSelectedIndex = PlayerPrefs.GetInt(leftId, 0);
        }
    }

    private string GetPlayerPrefsKeyForString(string s, bool isLeft)
    {
        return playerPrefKeyPrefix + s + (isLeft ? "_l" : "_r");
    }

    public void SetLastGlyphSelectedIndex(string key, int index, bool isLeft)
    {
        key = key.ToLower();

        if(!DiacriticsDictionary.ContainsKey(key))
        {
            DebugLogger.Log("DiacriticsDictionaryReference SetLastRightGlyphSelectedIndex call for a not existing key", DebugLogger.LogType.WARNING);
            return;
        }

        string id = GetPlayerPrefsKeyForString(key, isLeft);
        PlayerPrefs.SetInt(id, index);

        if(isLeft)
        {
            DiacriticsDictionary[key].LastLeftGlyphSelectedIndex = index;
        }
        else
        {
            DiacriticsDictionary[key].LastRightGlyphSelectedIndex = index;
        }
    }

    public void SetLastGlyphSelectedIndexForCurrentPrecedingCharacter(int index, bool isLeft)
    {
        string precedingCharacter = nativeInterface.GetPrecedingCharacter(1);
        if(string.IsNullOrEmpty(precedingCharacter) || nativeInterface.LastCommitWasEmoji) return;
        SetLastGlyphSelectedIndex(precedingCharacter, index, isLeft);
    }

    public string GetLastAlternateGlyphSelectedForCurrentPrecedingCharacter(bool left)
    {
        string precedingCharacter = nativeInterface.GetPrecedingCharacter(1);
        if(string.IsNullOrEmpty(precedingCharacter) || nativeInterface.LastCommitWasEmoji) return null;

        DiacriticsDictionaryEntry diacriticsDictionaryEntry;
        bool glyphFound = DiacriticsDictionary.TryGetValue(precedingCharacter.ToLower(), out diacriticsDictionaryEntry);
        if(!glyphFound) return null;

        int lastAlternateGlyphIndexSelected = left ? diacriticsDictionaryEntry.LastLeftGlyphSelectedIndex : diacriticsDictionaryEntry.LastRightGlyphSelectedIndex;
        string[] alternateGlyphs = left ? diacriticsDictionaryEntry.LeftGlyphs : diacriticsDictionaryEntry.RightGlyphs;

        if(alternateGlyphs == null || alternateGlyphs.Length == 0) return null;

        if (lastAlternateGlyphIndexSelected >= alternateGlyphs.Length)
        {
            lastAlternateGlyphIndexSelected = 0;
            if (left) diacriticsDictionaryEntry.LastLeftGlyphSelectedIndex = 0;
            else diacriticsDictionaryEntry.LastRightGlyphSelectedIndex = 0;
        }

        string lastAlternateGlyphSelected = alternateGlyphs[lastAlternateGlyphIndexSelected];

        if(string.IsNullOrEmpty(lastAlternateGlyphSelected)) return null;

        lastAlternateGlyphSelected = char.IsUpper(precedingCharacter[0]) ? lastAlternateGlyphSelected.ToUpper() : lastAlternateGlyphSelected.ToLower();

        return lastAlternateGlyphSelected;
    }

    public string[] GetAlternateGlyphsForCurrentPrecedingCharacter(bool left)
    {
        string precedingCharacter = nativeInterface.GetPrecedingCharacter(1);
        if(string.IsNullOrEmpty(precedingCharacter) || nativeInterface.LastCommitWasEmoji) return null;

        DiacriticsDictionaryEntry diacriticsDictionaryEntry;
        bool glyphFound = DiacriticsDictionary.TryGetValue(precedingCharacter.ToLower(), out diacriticsDictionaryEntry);
        if(!glyphFound) return null;

        string[] alternateGlyphs = new string[0];
        alternateGlyphs = left ? diacriticsDictionaryEntry.LeftGlyphs : diacriticsDictionaryEntry.RightGlyphs;

        if (alternateGlyphs != null)
        {
            for (int i = 0; i < alternateGlyphs.Length; i++)
            {
                alternateGlyphs[i] = char.IsUpper(precedingCharacter[0]) ? alternateGlyphs[i].ToUpper() : alternateGlyphs[i].ToLower();
            }
        }

        return alternateGlyphs;
    }
}
