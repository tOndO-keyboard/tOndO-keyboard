using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class LandscapeButtonLayoutController : MonoBehaviour
{
    [SerializeField]
    RectTransform rectTransform;

    [SerializeField]
    RectTransform rectTransformP;

    [SerializeField]
    RectTransform rectTransformL;

    private RectTransform canvasRectTransform;

    bool wasLandscape = false;

    private void Awake()
    {
        canvasRectTransform = GetComponentInParent<Canvas>().GetComponent<RectTransform>();
    }

    private void Update()
    {
        bool isLandscape = NativeInterface.Instance.IsLandscape() && 
                canvasRectTransform.rect.width / canvasRectTransform.rect.height > CanvasScalerManager.LANDSCAPE_PROPORTION;

        if (wasLandscape != isLandscape)
        {
            rectTransform.anchorMax = isLandscape ? rectTransformL.anchorMax : rectTransformP.anchorMax;
            rectTransform.anchorMin = isLandscape ? rectTransformL.anchorMin : rectTransformP.anchorMin;

            rectTransform.anchoredPosition = isLandscape ? rectTransformL.anchoredPosition : rectTransformP.anchoredPosition;
        }

        wasLandscape = isLandscape;
    }
}
