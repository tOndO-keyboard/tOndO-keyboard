using System.Collections;
using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(ScrollRect))]
public class AutoScroller : MonoBehaviour
{
    [SerializeField, Range(0, 1)]
    private float _scrollTime = .2f;

    [SerializeField, Range(0, .001f)]
    private float _scrollOverflow = .0004f;

    private ScrollRect _scrollRect = null;

    private Coroutine _coroutine = null;

    public ScrollRect Scroller => _scrollRect ?? (_scrollRect = GetComponent<ScrollRect>());

    public bool IsScrolling => _coroutine != null;

    private IEnumerator ScrollRoutine(float to)
    {
        float from = Scroller.verticalNormalizedPosition;

        if(from < to)
        {
            to += _scrollOverflow;
        }
        else
        {
            to -= _scrollOverflow;
        }

        for(float timer = 0; timer <= _scrollTime; timer += Time.unscaledDeltaTime)
        {
            Scroller.verticalNormalizedPosition = Mathf.Lerp(from, to, timer / _scrollTime);
            yield return null;
        }
        Scroller.verticalNormalizedPosition = to;
        _coroutine = null;

    }

    public void ScrollTo(float normalizedY)
    {
        if (IsScrolling) StopCoroutine(_coroutine);
        _coroutine = StartCoroutine(ScrollRoutine(normalizedY));
    }
}
