using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ToastAndRedirectLockOverride : LockOverride
{
    [SerializeField, Min(0)]
    private float _secondTapWindow = 2;

    private bool _firstTap = false;

    private WaitForSeconds _wait = null;
    private Coroutine _window = null;

    private void Awake()
    {
        _wait = new WaitForSeconds(_secondTapWindow);
    }

    private void OnDisable()
    {
        if (_window != null) StopCoroutine(_window);
        _window = null;
        _firstTap = false;
    }

    private IEnumerator WindowCoroutine()
    {
        _firstTap = true;
        yield return _wait;
        _firstTap = false;
        _window = null;
    }

    public override void LockFunction()
    {
        if (!_firstTap) 
        {
            _window = StartCoroutine(WindowCoroutine());
            Toast.Instance.Show(Localization.Instance.Localize(Localization.PRO_REQUIRED_MESSAGE));
        }
        else 
        {
            StopCoroutine(_window);
            _window = null;
            _firstTap = false;
            NativeInterface.Instance.OpenKeyboardOptions();
        }
    }
}
