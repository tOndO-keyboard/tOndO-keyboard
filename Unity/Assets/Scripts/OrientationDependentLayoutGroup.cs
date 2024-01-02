using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(HorizontalOrVerticalLayoutGroup))]
[ExecuteInEditMode]
public class OrientationDependentLayoutGroup : MonoBehaviour
{
    [SerializeField]
    private bool _expandWidthInPortrait = true;

    [SerializeField]
    private float _spacingInPortrait = 20.0f;

    [SerializeField]
    private float _spacingInLandscape = 87.0f;


    private RectTransform _canvasRectTransform;
    private HorizontalOrVerticalLayoutGroup _layoutGroup;

    private void Awake()
    {
        _canvasRectTransform = transform.root.GetComponentInChildren<Canvas>().GetComponent<RectTransform>();
        _layoutGroup = GetComponent<HorizontalOrVerticalLayoutGroup>();
    }

    private void OnEnable() => Update();

    private void Update()
    {
        bool isLandscape = _canvasRectTransform.rect.width / _canvasRectTransform.rect.height > CanvasScalerManager.LANDSCAPE_PROPORTION;
        _layoutGroup.childForceExpandWidth = _expandWidthInPortrait && !isLandscape;
        _layoutGroup.spacing = isLandscape ? _spacingInLandscape : _spacingInPortrait;
    }
}
