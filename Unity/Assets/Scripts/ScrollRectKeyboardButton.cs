using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public abstract class ScrollRectKeyboardButton : KeyboardButton, IDragHandler, IBeginDragHandler, IEndDragHandler
{
    [SerializeField]
    private ScrollRect scrollRect;
    [SerializeField]
    private bool _silentMissingScrollRect = false;

    public ScrollRect ScrollRect
    {
        get
        {
            if(scrollRect == null)
            {
                scrollRect = GetComponentInParent<ScrollRect>();
            }

            return scrollRect;
        }

        set
        {
            scrollRect = value;
        }
    }

    public virtual void OnDrag(PointerEventData eventData)
    {
        if (_silentMissingScrollRect && ScrollRect == null) return;
        inputCancelled = true;
        ScrollRect.OnDrag(eventData);
    }

    public void OnEndDrag(PointerEventData eventData)
    {
        if (_silentMissingScrollRect && ScrollRect == null) return;
        ScrollRect.OnEndDrag(eventData);
    }
    public void OnBeginDrag(PointerEventData eventData)
    {
        if (_silentMissingScrollRect && ScrollRect == null) return;
        inputCancelled = true;
        ScrollRect.OnBeginDrag(eventData);
    }
}
