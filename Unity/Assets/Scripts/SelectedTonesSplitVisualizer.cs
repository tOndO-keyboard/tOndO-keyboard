using UnityEngine;
using UnityEngine.UI;

public class SelectedTonesSplitVisualizer : MonoBehaviour
{
    [SerializeField]
    private Image _tone1Image = null;
    [SerializeField]
    private Image _tone2Image = null;
    [SerializeField]
    private Sprite _toneSprite = null;
    [SerializeField, Range(0, 1)]
    private float _unselectedScale = .8f;

    private void Awake()
    {
        if (_toneSprite == null) return;
        _tone1Image.sprite = _toneSprite;
        _tone2Image.sprite = _toneSprite;
    }

    public void SetSelected(bool upper, bool lower)
    {
        bool unselected = !upper && !lower;
        _tone1Image.enabled = upper || unselected;
        _tone2Image.enabled = lower || unselected;

        transform.localScale = Vector3.one;
        if (unselected) transform.localScale *= _unselectedScale;
    }

#if UNITY_EDITOR
    private void OnValidate()
    {
        if (_tone1Image != null) _tone1Image.sprite = _toneSprite;
        if (_tone2Image != null) _tone2Image.sprite = _toneSprite;
    }
#endif
}
