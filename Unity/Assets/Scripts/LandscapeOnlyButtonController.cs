using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(CanvasGroup))]
public class LandscapeOnlyButtonController : MonoBehaviour
{
    private CanvasGroup canvasGroup;
    private RectTransform canvasRectTransform;
    private LayoutElement layoutElement;
    private bool wasLandscape;

    private CanvasGroup CanvasGroup => canvasGroup ?? (canvasGroup = GetComponent<CanvasGroup>());
    private RectTransform CanvasRectTransform
    {
        get
        {
            if (canvasRectTransform == null)
            {
                canvasRectTransform = transform.root
                        .GetComponentInChildren<Canvas>()
                        .GetComponent<RectTransform>();
            }
            return canvasRectTransform;
        }
    }
    private LayoutElement LayoutElement => layoutElement ?? (layoutElement = GetComponent<LayoutElement>());

    private void OnEnable()  => Update();

    private void Update()
    {
        bool isLandscape = NativeInterface.Instance.IsLandscape() && 
                CanvasRectTransform.rect.width / CanvasRectTransform.rect.height > CanvasScalerManager.LANDSCAPE_PROPORTION;
        if (wasLandscape != isLandscape)
        {
            CanvasGroup.alpha = isLandscape ? 1 : 0;
            CanvasGroup.interactable = isLandscape;
            CanvasGroup.blocksRaycasts = isLandscape;

            if(LayoutElement != null)
            {
                LayoutElement.ignoreLayout = !isLandscape;
            }
        }

        wasLandscape = isLandscape;
    }
}
