using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class DoneButton : KeyboardButton
{
    public enum DONE_ACTION_FLAG { DONE = 0, NEWLINE = 1, SEARCH = 2, SEND = 3 }

    public delegate void DoneCommittedEventDelegate(DoneButton source, DONE_ACTION_FLAG flag);

    public static event DoneCommittedEventDelegate DoneCommitted;

    [SerializeField]
    private Image icon;

    [SerializeField]
    private SDictionaryDoneActionFlagSprite doneSprites;

    private DONE_ACTION_FLAG currentFlag = DONE_ACTION_FLAG.NEWLINE;

    private void Awake()
    {
        SetDoneSprite();
    }

    private void OnApplicationPause(bool pause)
    {
        if(!pause) SetDoneSprite();
    }

    private void SetDoneSprite()
    {
        currentFlag = (DONE_ACTION_FLAG)NativeInterface.GetDoneActionFlag();
        if (doneSprites.ContainsKey(currentFlag)) icon.sprite = doneSprites[currentFlag];
    }

    public override void OnKeyTrigger()
    {
        NativeInterface.CommitDone();
        DoneCommitted?.Invoke(this, currentFlag);
    }
}
