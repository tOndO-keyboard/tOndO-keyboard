using System.Collections;
using UnityEngine;
using UnityEngine.Events;
using UnityEngine.UI;

[System.Serializable]
public class OnCategoryClickEvent : UnityEvent<float> { }

public class CategoryButton : KeyboardButton
{
    [SerializeField]
    private bool _startSelected = false;
    [SerializeField]
    private float _transitionTime = .2f;

    [Space]
    [SerializeField]
    private CategoryHighlightController _highlight = null;
    [SerializeField]
    private CategoryContainer _categoryContainer = null;

    [Space]
    [SerializeField]
    private OnCategoryClickEvent _onClick;

    private Coroutine _selectingCoroutine = null;

    public RectTransform RectTransform => transform as RectTransform;

    public bool StartsSelected => _startSelected;
    public bool IsSelected { get; private set; }

    public CategoryContainer CategoryContainer
    {
        get => _categoryContainer;
        set
        {
            _categoryContainer = value;
            Init();
        }
    }

    private void Init()
    {
        if (_categoryContainer != null)
            Button.image.sprite = _categoryContainer.Icon;
    }

    private void Awake() => Init();

    private void Start()
    {
        if (_startSelected)
        {
            SetSelected(_startSelected, true);
        }
        else
        {
            Color color = Button.image.color;
            color.a =  .5f;
            Button.image.color = color;
        }
    }

    private IEnumerator SelectCoroutine(bool select, bool instant)
    {
        IsSelected = select;

        Color colorFrom = Button.image.color;
        Color colorTo = Button.image.color;
        colorTo.a = select ? 1 : .5f;

        for(float timer = 0; timer <= _transitionTime && !instant; timer += Time.unscaledDeltaTime)
        {
            float t = timer / _transitionTime;
            Color color = Color.Lerp(colorFrom, colorTo, t);
            Button.image.color = color;
            yield return null;
        }
        Button.image.color = colorTo;
        _selectingCoroutine = null;
    }

    public void SetSelected(bool select, bool instant)
    {
        if (_highlight != null) _highlight.Select(this, instant);
        if (_selectingCoroutine != null) StopCoroutine(_selectingCoroutine);
        _selectingCoroutine = StartCoroutine(SelectCoroutine(select, instant));
    }

    public void OnScrollRectChange(Vector2 v)
    {
        if (_categoryContainer == null) return;
        if (v.y >= _categoryContainer.NormalizedExtentY && v.y <= _categoryContainer.NormalizedY) 
        {
            if (!IsSelected) 
                SetSelected(true, false);
        }
        else if (IsSelected) SetSelected(false, false);
    }

    public override void OnKeyTrigger() => 
        //Little offset to be sure it's considered as selected
        _onClick.Invoke(_categoryContainer.NormalizedY);
}
