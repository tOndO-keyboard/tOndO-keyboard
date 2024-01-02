using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using TMPro;
using UnityEngine.UI;

public class Toast : LazySingleIstanceMonoBehaviour<Toast>
{
    [SerializeField]
    private CanvasGroup canvasGroup;

    [SerializeField]
    private TMP_Text text;

    [SerializeField]
    private float duration = 4.0f;

    [SerializeField]
    private float fadeDuration = 0.1f;

    public void Show(string text)
    {
        this.text.text = text;
        StopCoroutine("show");
        StartCoroutine("show");
    }

    private IEnumerator show()
    {
        yield return tweenAlpha(1);
        yield return new WaitForSeconds(duration);
        yield return tweenAlpha(0);
    }

    private IEnumerator tweenAlpha(float to)
    {
        float start = Time.time;
        while (!MathUtils.ApproximatelyEquals(canvasGroup.alpha, to, 0.01f))
        {
            float elapsed = Time.time - start;
            float normalisedTime = Mathf.Clamp((elapsed / fadeDuration) * Time.deltaTime, 0, 1);
            canvasGroup.alpha = Mathf.Lerp(canvasGroup.alpha, to, normalisedTime);
            yield return 0;
        }
        canvasGroup.alpha = to;
        yield return true;
    }

}
