using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class RadialLayout : LayoutGroup
{
    [SerializeField]
    public float distance;
    private enum From { Center, Width, Height, AverageWH };

    [SerializeField]
    private From from;

    [Range(0f, 360f)]
    public float MinAngle, MaxAngle, StartAngle;

    protected override void OnEnable() { base.OnEnable(); CalculateRadial(); }
    
    public override void SetLayoutHorizontal()
    {
    }
    
    public override void SetLayoutVertical()
    {
    }
    
    public override void CalculateLayoutInputVertical()
    {
        CalculateRadial();
    }
    
    public override void CalculateLayoutInputHorizontal()
    {
        CalculateRadial();
    }

/*#if UNITY_EDITOR
    protected override void OnValidate()
    {
        base.OnValidate();
        CalculateRadial();
    }
#endif*/

    public void CalculateRadial()
    {
        m_Tracker.Clear();
        if (transform.childCount == 0)
            return;

        List<Transform> children = new List<Transform>();

        for (int i = 0; i < transform.childCount; i++)
        {
            Transform child = transform.GetChild(i);
            LayoutElement layoutElement = child.GetComponent<LayoutElement>();
            if (!(layoutElement != null && layoutElement.ignoreLayout))
            {
                children.Add(child);
            }
        }

        float fOffsetAngle = ((MaxAngle - MinAngle)) / (children.Count);

        float fAngle = StartAngle;
        for (int i = 0; i < children.Count; i++)
        {
            RectTransform child = (RectTransform)children[i];
            if (child != null)
            {
                LayoutElement layoutElement = child.GetComponent<LayoutElement>();
                if (! (layoutElement != null && layoutElement.ignoreLayout))
                {
                    m_Tracker.Add(this, child,
                    DrivenTransformProperties.Anchors |
                    DrivenTransformProperties.AnchoredPosition |
                    DrivenTransformProperties.Pivot);
                    Vector3 vPos = new Vector3(Mathf.Cos(fAngle * Mathf.Deg2Rad), Mathf.Sin(fAngle * Mathf.Deg2Rad), 0);
                   
                    float fromD = 0;
                    float sign = 1;

                    if (from == From.Center) sign = -1;
                    else if (from == From.Width) fromD = rectTransform.rect.width * 0.5f;
                    else if(from == From.Height) fromD = rectTransform.rect.height * 0.5f;
                    else if (from == From.AverageWH) fromD = ((rectTransform.rect.width + rectTransform.rect.height ) * 0.5f) * 0.5f;

                    child.localPosition = vPos * (fromD - (distance * sign) );
                    child.anchorMin = child.anchorMax = child.pivot = new Vector2(0.5f, 0.5f);
                    fAngle += fOffsetAngle;
                }
            }
        }

    }
}