using System.Collections;
using UnityEngine;

public class CategoryHighlightController : MonoBehaviour
{
    [SerializeField]
    private float _transitionTime = .2f;
    [SerializeField]
    private bool _elastic = false;
    [SerializeField]
    private float _verticalOffset = -45;

    private Coroutine _coroutine = null;

    public RectTransform RectTransform => transform as RectTransform;

    private IEnumerator SelectCoroutine(float y, float to, bool instant)
    {
        float from = RectTransform.anchoredPosition.x;
        Vector2 pos = RectTransform.anchoredPosition;
        pos.y = y;
        for (float timer = 0; timer <= _transitionTime && !instant; timer += Time.unscaledDeltaTime)
        {
            if (_elastic) pos.x = MathUtils.ElasticLerp(from, to, timer / _transitionTime);
            else pos.x = Mathf.Lerp(from, to, timer / _transitionTime);
            RectTransform.anchoredPosition = pos;
            yield return null;
        }

        pos.x = to;
        RectTransform.anchoredPosition = pos;
        _coroutine = null;
    }

    public void Select(CategoryButton button, bool instant)
    {
        if (_coroutine != null) StopCoroutine(_coroutine);
        var buttonPos = button.RectTransform.anchoredPosition;
        _coroutine = StartCoroutine(SelectCoroutine(buttonPos.y + _verticalOffset, buttonPos.x, instant));
    }
}
