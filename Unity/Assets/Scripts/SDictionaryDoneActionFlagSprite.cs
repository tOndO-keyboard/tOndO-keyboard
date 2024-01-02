using System;
using System.Collections.Generic;
using UnityEngine;

[Serializable]
public class SDictionaryDoneActionFlagSprite : SDictionary<DoneButton.DONE_ACTION_FLAG, Sprite>
{
    [SerializeField]
    private List<DoneButton.DONE_ACTION_FLAG> serializedKeys;

    protected override List<DoneButton.DONE_ACTION_FLAG> SerializedKeys
    {
        get { return serializedKeys; }
        set { serializedKeys = value; }
    }

    [SerializeField]
    private List<Sprite> serializedValues;

    protected override List<Sprite> SerializedValues
    {
        get { return serializedValues; }
        set { serializedValues = value; }
    }
}
