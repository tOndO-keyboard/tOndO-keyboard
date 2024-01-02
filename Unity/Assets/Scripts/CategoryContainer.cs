using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class CategoryContainer : MonoBehaviour
{
    [SerializeField]
    private ScrollRect _scroll = null;
    [Space]
    [SerializeField]
    private Sprite _categoryIcon = null;

    private RectTransform RectTransform => transform as RectTransform;

    private float NavigableHeight 
    {
        get
        {
            RectTransform scrollRect = _scroll.transform as RectTransform;
            RectTransform parent = transform.parent as RectTransform;
            return parent.rect.height - scrollRect.rect.height;
        }
    }

    public ScrollRect ScrollRect
    {
        get => _scroll;
        set
        {
            _scroll = value;
            transform.SetParent(_scroll.viewport.GetChild(0));
        }
    }

    public float NormalizedY
    {
        get
        {
            float y = -RectTransform.anchoredPosition.y;
            return 1 - y / NavigableHeight;
        }
    }

    public float NormalizedExtentY
    {
        get
        {
            float y = - RectTransform.anchoredPosition.y;
            float extentY = y + RectTransform.rect.height;
            return 1 - extentY / NavigableHeight;
        }
    }

    public bool IsVisible => 
        _scroll.verticalNormalizedPosition >= NormalizedExtentY && 
        _scroll.verticalNormalizedPosition <= NormalizedY;

    public Sprite Icon 
    {
        get => _categoryIcon;
        set => _categoryIcon = value;
    }
}
