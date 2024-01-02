using System.Linq;
using System.Collections.Generic;
using UnityEngine;

[RequireComponent(typeof(RectTransform))]
public class SinusoidalIcon : MonoBehaviour
{
    private enum WaveDirection { Vertical = 1, Horizontal = 2 }

    private static Vector2 WaveDirectionToVectorDirection(WaveDirection dir)
    {
        int x = ((int)dir & (int)WaveDirection.Horizontal) >> 1;
        int y = (int)dir & (int)WaveDirection.Vertical;
        return new Vector2(x, y);
    }

    [SerializeField]
    private float _amplitude = 10;
    [SerializeField]
    private float _frequency = .25f;
    [SerializeField]
    private float _phaseQuantization = .1f;
    [SerializeField]
    private WaveDirection _direction = WaveDirection.Vertical;

    private List<RectTransform> _children;
    private List<Vector2> _starts;

    public RectTransform RectTransform => transform as RectTransform;

    private void Awake()
    {
        _children = transform
            .Cast<Transform>()
            .Where(t => t is RectTransform)
            .Cast<RectTransform>()
            .ToList();
        _starts = _children
            .Select(t => t.anchoredPosition)
            .ToList();
    }

    private void OnEnable() => Update();

    private void Update()
    {
        float omegaT = _frequency * Time.time * 2 * Mathf.PI;
        for (int i = 0; i < _children.Count; i++)
        {
            var rect = _children[i];
            var start = _starts[i];
            Vector2 offset = _amplitude * Mathf.Sin(omegaT - _phaseQuantization * i) * WaveDirectionToVectorDirection(_direction);
            rect.anchoredPosition = start +  offset;
        }
    }
}
