using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public abstract class ViewDependantButton : KeyboardButton
{
     [SerializeField]
    protected KeyboardController _controller = null;
    [SerializeField]
    private KeyboardController.View _view = KeyboardController.View.Main;
    [SerializeField]
    protected Image _icon = null;
    [SerializeField]
    protected Sprite _inactiveSprite = null;
    [SerializeField]
    protected Sprite _activeSprite = null;

    protected virtual void OnEnable() => _controller.ViewChanged += OnViewChanged;

    protected virtual void OnDisable() => _controller.ViewChanged -= OnViewChanged;

    private void OnViewChanged(KeyboardController.View view)
    {
        if (view == _view)
            _icon.sprite = _activeSprite;
        else _icon.sprite = _inactiveSprite;
    }
}
