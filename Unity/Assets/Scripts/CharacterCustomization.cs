using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class CharacterCustomization :  LazySingleIstanceMonoBehaviour<CharacterCustomization>
{
    private Dictionary<LayoutType, Dictionary<AnalogStickPosition, Dictionary<CardinalDirection, string>>> characterMapDictionary;

    private Dictionary<LayoutType, Dictionary<AnalogStickPosition, Dictionary<CardinalDirection, string>>> CharacterMapDictionary
    {
        get
        {
            deserialize();
            return characterMapDictionary;
        }
    }

    private string lastDeserializedCharacterCustomizationString = String.Empty;

    public string Get(LayoutType layoutType, AnalogStickPosition analogStickPosition, CardinalDirection cardinalDirection)
    {
        if(!CharacterMapDictionary.ContainsKey(layoutType))
        {
            DebugLogger.Log("CharacterMapDictionary does not contain any " + Enum.GetName(typeof(LayoutType), layoutType) + " key", DebugLogger.LogType.INFO);
            return "";
        }

        if (!CharacterMapDictionary[layoutType].ContainsKey(analogStickPosition))
        {
            DebugLogger.Log("CharacterMapDictionary[" + Enum.GetName(typeof(LayoutType), layoutType) + "] does not contain any " + Enum.GetName(typeof(AnalogStickPosition), analogStickPosition) + " key", DebugLogger.LogType.INFO);
            return "";
        }

        if (!CharacterMapDictionary[layoutType][analogStickPosition].ContainsKey(cardinalDirection))
        {
            DebugLogger.Log("CharacterMapDictionary[" + Enum.GetName(typeof(LayoutType), layoutType) + "][" + Enum.GetName(typeof(AnalogStickPosition), analogStickPosition) + "] does not contain any " + Enum.GetName(typeof(CardinalDirection), cardinalDirection) + " key", DebugLogger.LogType.INFO);
            return "";
        }

        return CharacterMapDictionary[layoutType][analogStickPosition][cardinalDirection];
    }

    private static readonly LayoutType[] layoutTypeOrder = { LayoutType.Main, LayoutType.Symbols1, LayoutType.Symbols2 };
    private static readonly AnalogStickPosition[] analogStickPositionOrder = {  AnalogStickPosition.TopLeft, AnalogStickPosition.TopCenter, AnalogStickPosition.TopRight,
                                                                                AnalogStickPosition.BottomLeft, AnalogStickPosition.BottomCenter, AnalogStickPosition.BottomRight   };
    private static readonly CardinalDirection[] cardinalDirectionOrder = {  CardinalDirection.Center, CardinalDirection.West, CardinalDirection.SouthWest, CardinalDirection.SouthEast,
                                                                            CardinalDirection.East, CardinalDirection.NorthEast, CardinalDirection.NorthWest};

    private void deserialize()
    {
        string characterCustomizationString = SettingsManager.Instance.CharacterCustomizationString;

        if(characterCustomizationString.Equals(lastDeserializedCharacterCustomizationString))
        {
            return;
        }

        lastDeserializedCharacterCustomizationString = characterCustomizationString;

        characterMapDictionary = new Dictionary<LayoutType, Dictionary<AnalogStickPosition, Dictionary<CardinalDirection, string>>>();

        string[] layoutTypeStrings = characterCustomizationString.Split(new string[] { "\n\n" }, StringSplitOptions.None);

        for (int i = 0; i < layoutTypeOrder.Length && i < layoutTypeStrings.Length; i++)
        {
            string[] analogStickPositionStrings = layoutTypeStrings[i].Split('\n');
            if(layoutTypeOrder[i] == LayoutType.Main) //main keyboard should follow shift state for case, we are forcing uppercase by defaault
            {
                layoutTypeStrings[i] = layoutTypeStrings[i].ToUpper();
            }

            Dictionary<AnalogStickPosition, Dictionary<CardinalDirection, string>> analogStickPositionDictionary = new Dictionary<AnalogStickPosition, Dictionary<CardinalDirection, string>>();

            for (int j = 0; j < analogStickPositionOrder.Length && j < analogStickPositionStrings.Length; j++)
            {
                char[] cardinalDirectionChars = analogStickPositionStrings[j].ToCharArray();

                Dictionary<CardinalDirection, string> cardinalDirectionDictionary = new Dictionary<CardinalDirection, string>();

                for (int k = 0; k < cardinalDirectionOrder.Length && k < cardinalDirectionChars.Length; k++)
                {
                    int cardinalDirectionOrderIndex = k;
                    if(analogStickPositionOrder[j] == AnalogStickPosition.TopRight && k == 5) //we need to skip NorthEast in the top right tondos because it's the place for diacriticizers
                    {
                        cardinalDirectionOrderIndex++;
                    }
                    if (!cardinalDirectionDictionary.ContainsKey(cardinalDirectionOrder[cardinalDirectionOrderIndex]))
                    {
                        cardinalDirectionDictionary.Add(cardinalDirectionOrder[cardinalDirectionOrderIndex], cardinalDirectionChars[k].ToString());
                    }
                }

                analogStickPositionDictionary.Add(analogStickPositionOrder[j], cardinalDirectionDictionary);
            }

            characterMapDictionary.Add(layoutTypeOrder[i], analogStickPositionDictionary);
        }
    }
}
