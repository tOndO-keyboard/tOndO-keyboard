using System.Collections.Generic;
using System.Linq;
using UnityEngine;
using UnityEngine.UI;

public class MacrosController : MonoBehaviour
{
    [SerializeField]
    private ClipboardCommitButton _clipboardButton = null;

    private ProLock _proLock;

    [Space]
    [SerializeField, Min(0)]
    private int _macrosOverride = 0;

    [Space]
    [SerializeField]
    private RectTransform _macrosRoot = null;
    [SerializeField]
    private List<PinButtonsContainer> _pinContainers = new List<PinButtonsContainer>();

    private void AssignText(ClipboardCommitButton button, string value)
    {
        if (string.IsNullOrEmpty(value))
        {
            button.gameObject.SetActive(false);
            bool allSibilingsInactive = true;

            if (button.transform.parent == null) return;

            foreach (Transform t in button.transform.parent)
            {
                allSibilingsInactive &= !t.gameObject.activeSelf;
                if (!allSibilingsInactive) break;
            }
            button.transform.parent.gameObject.SetActive(!allSibilingsInactive);
            return;
        }

        button.transform.parent.gameObject.SetActive(true);
        button.gameObject.SetActive(true);
        button.Text = value;
    }

    private void Repopulate()
    {
        var macros = MacrosPersister.Instance.Keys.ToList();
        int buttonIndex = 0;
        int count = _macrosOverride > 0 ? Mathf.Min(_macrosOverride, macros.Count) : macros.Count;

        if (NativeInterface.Instance.IsPro())
        {
            count = macros.Count;
        }

        for (int i = 0; i < _pinContainers.Count; i++)
        {
            PinButtonsContainer c = _pinContainers[i];
            bool thereIsTutorial = false;
            foreach (ClipboardCommitButton b in c)
            {
                string value = null;
                if (buttonIndex < count) 
                {
                    value = macros[buttonIndex];
                    thereIsTutorial |= MacrosPersister.Instance.IsTutorial(value);
                    buttonIndex++;
                }
                AssignText(b, value);
            }
            c.SetForFullText(thereIsTutorial);
        }
        LayoutRebuilder.ForceRebuildLayoutImmediate(_macrosRoot);

        CheckPro();
    }

    private void OnClipboardChange(string value) => AssignText(_clipboardButton, value);

    private void OnLongPressTrigger(ClipboardCommitButton button, bool isFunctionActive)
    {
        if (_clipboardButton != button) _clipboardButton.IsFunctionActive = false;
        foreach (var b in _pinContainers.SelectMany(c => c))
            if (b != button) b.IsFunctionActive = false;
    }

    private void Awake()
    {
        _proLock = _clipboardButton.GetComponent<ProLock>();

        if (_macrosOverride > 0)
            _clipboardButton.MacrosOverride = _macrosOverride;
    }

    private int GetClipboardCommitButtonCount()
    {
        int count = 0;
        foreach(PinButtonsContainer pinContainer in _pinContainers)
        {
            foreach(ClipboardCommitButton commitButton in pinContainer)
            {
                count++;
            }
        }
        return count;
    }

    private void OnEnable()
    {
        MacrosPersister.Instance.Capacity = GetClipboardCommitButtonCount();
        NativeInterface.ClipboardChange += OnClipboardChange;
        MacrosPersister.Instance.Changed += Repopulate;

        _clipboardButton.LongPress += OnLongPressTrigger;
        foreach (var c in _pinContainers)
        {
            foreach (var mb in c) 
                mb.LongPress += OnLongPressTrigger;
        }

        OnClipboardChange(NativeInterface.Instance.GetClipboardContent());
        Repopulate();
    }

    private void Update() => OnClipboardChange(NativeInterface.Instance.GetClipboardContent());

    private void OnDisable()
    {
        NativeInterface.ClipboardChange -= OnClipboardChange;
        MacrosPersister.Instance.Changed -= Repopulate;

        _clipboardButton.LongPress -= OnLongPressTrigger;
        foreach (var c in _pinContainers)
        {
            foreach (var mb in c) 
                mb.LongPress -= OnLongPressTrigger;
        }
    }

    private void OnApplicationPause(bool paused)
    {
        if (paused) MacrosPersister.SaveInstance();
    }

    private void CheckPro()
    {
        if (NativeInterface.Instance.IsPro())
        {
            _clipboardButton.MacrosOverride = 0;
        }
        else
        {
            _clipboardButton.MacrosOverride = _macrosOverride;
            _proLock.ForceDisable = MacrosPersister.Instance.Count < _macrosOverride;
        }
    }
}
