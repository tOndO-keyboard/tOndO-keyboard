using System;
using UnityEngine;
using UnityEngine.EventSystems;

public class BackspaceButton : KeyboardButton
{
    [SerializeField]
    private float initialEreaseDelay = 0.5f;

    [SerializeField]
    private float ereaseDelay = 0.05f;

    private bool selected = false;
    private float lastEreaseTime = 0;
    private float firstPressTime = 0;
    private bool deletingWords = false;

    private void DeleteLastCharacter()
    {
        NativeInterface.CommitBackspace();
        lastEreaseTime = Time.time;
    }

    public void DeleteLastWord()
    {
        string precedingCharacter = NativeInterface.GetPrecedingCharacter(1);

        if(!string.IsNullOrEmpty(precedingCharacter) && IsPunctuationSpaceOrNewLine(precedingCharacter))
        {
            while (!string.IsNullOrEmpty(precedingCharacter) && IsPunctuationSpaceOrNewLine(precedingCharacter))
            {
                NativeInterface.CommitBackspace();
                precedingCharacter = NativeInterface.GetPrecedingCharacter(1, true);
                deletingWords = true;
            }
        }
        
        while(!string.IsNullOrEmpty(precedingCharacter) && !IsPunctuationSpaceOrNewLine(precedingCharacter))
        {
            NativeInterface.CommitBackspace(); 
            precedingCharacter = NativeInterface.GetPrecedingCharacter(1, true);
            deletingWords = true;
        }
    }

    private static bool IsPunctuationSpaceOrNewLine(string s)
    {
        if (string.IsNullOrEmpty(s)) { return false; }
        char c = s[0];
        return char.IsPunctuation(c) || s.Equals(" ") || s.Equals(Environment.NewLine);
    }
    
    private void FixedUpdate()
    {
        if(selected)
        {
            if (Time.time - firstPressTime < initialEreaseDelay)
            {
                return;
            }
                
            if (Time.time - lastEreaseTime > ereaseDelay)
            {
                DeleteLastCharacter();
            }
        }
    }

    public override void OnPointerDown(PointerEventData eventData)
    {
        base.OnPointerDown(eventData);
        firstPressTime = Time.time;
        lastEreaseTime = 0;
        deletingWords = false;
        selected = true;
    }

    public override void OnPointerUp(PointerEventData eventData)
    {
        base.OnPointerUp(eventData);

        if (selected && Time.time - firstPressTime < initialEreaseDelay && !deletingWords)
        {
            DeleteLastCharacter();
        }

        selected = false;
    }

    public override void OnPointerExit(PointerEventData eventData)
    {
        selected = false;
        base.OnPointerExit(eventData);
    }

    public override void OnKeyTrigger()
    {

    }
}
