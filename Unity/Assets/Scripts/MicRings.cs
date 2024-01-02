using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class MicRings : MonoBehaviour
{
    [SerializeField]
    private Vector2 _startDimensions = new Vector2(300, 300);
    [SerializeField]
    private float _expansionSpeed = 20;
    [SerializeField]
    private float _fadeTime = 2;
    [SerializeField]
    private float _emissionInterval = .8f;

    [SerializeField, Range(0, 1)]
    private float _randomMargin = .05f;
    [SerializeField, Min(0)]
    private float _triggerFactor = 3;

    [Space]
    [SerializeField, Tooltip("Maximum number of instantiated rings. 0 means no limit."), Min(0)]
    private int _maxRings = 0;
    [SerializeField]
    private List<Image> _rings = new List<Image>();

    private float _emissionTimer = 0;
    private Queue<Image> _waiting = new Queue<Image>();
    private Dictionary<Image, Coroutine> _playing = new Dictionary<Image, Coroutine>();

    private float GetValueInMargin(float value)
    {
        float margin = _randomMargin * value;
        return Random.Range(-margin, margin) + value;
    }

    private void AdjustImageAlpha(Image img, float alpha)
    {
        Color c = img.color;
        c.a = alpha;
        img.color = c;
    }

    private void ResetTimer() => _emissionTimer = GetValueInMargin(_emissionInterval);

    private void EmitCircle(float triggerFactor = 1)
    {
        if (_waiting.Count == 0)
        {
            if (_maxRings != 0 && _rings.Count == _maxRings) return;
            var newRing = Instantiate(_rings[0], _rings[0].transform.parent);
            _rings.Add(newRing);
            _waiting.Enqueue(newRing);
        }

        var ring = _waiting.Dequeue();
        ring.rectTransform.sizeDelta = _startDimensions;
        AdjustImageAlpha(ring, 1);
        ring.rectTransform.anchoredPosition = Vector2.zero;

        float speed = GetValueInMargin(_expansionSpeed * triggerFactor);
        float time = GetValueInMargin(_fadeTime);

        _playing.Add(ring, StartCoroutine(RingCoroutine(ring, speed, time)));
    }

    private IEnumerator RingCoroutine(Image ring, float speed, float fadeTime)
    {
        for (float timer = 0; timer <= fadeTime; timer += Time.unscaledDeltaTime)
        {
            ring.rectTransform.sizeDelta += Time.unscaledDeltaTime * speed * Vector2.one;
            AdjustImageAlpha(ring, Mathf.Lerp(1, 0, timer / fadeTime));
            yield return null;
        }

        AdjustImageAlpha(ring, 0);
        _playing.Remove(ring);
        _waiting.Enqueue(ring);
    }

    private void OnEnable() 
    {
        _rings.ForEach(r => {
            AdjustImageAlpha(r, 0);
            _waiting.Enqueue(r);
        });

        ResetTimer();
    }

    private void OnDisable() {
        foreach (var c in _playing.Values) StopCoroutine(c);
        _playing.Clear();
        _waiting.Clear();
    }

    private void Update()
    {
        _emissionTimer -= Time.unscaledDeltaTime;
        if (_emissionTimer <= 0)
        {
            ResetTimer();
            EmitCircle();
        }
    }

    public void TriggerWithFactor() => EmitCircle(_triggerFactor);
}
