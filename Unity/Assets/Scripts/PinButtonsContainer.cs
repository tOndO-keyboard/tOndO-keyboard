using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class PinButtonsContainer : MonoBehaviour, IEnumerable<ClipboardCommitButton>
{
    [System.Serializable]
    private struct ButtonInfo
    {
        public ClipboardCommitButton Button;
        public LayoutElement LayoutElement;

        public void SetTextHeight(float heighy)
        {
            LayoutElement.minHeight = heighy;
        }
    }

    [SerializeField]
    private ButtonInfo _leftButton = default;
    [SerializeField]
    private ButtonInfo _rightButton = default;
    [SerializeField]
    private float _fullTextHeight = 300.0f;
    [SerializeField]
    private float _normalTextHeight = 100.0f;

    public void SetForFullText(bool fitFullText)
    {
        float height = fitFullText ? _fullTextHeight : _normalTextHeight;
        _leftButton.SetTextHeight(height);
        _rightButton.SetTextHeight(height);
    }

    public IEnumerator<ClipboardCommitButton> GetEnumerator()
    {
        yield return _leftButton.Button;
        yield return _rightButton.Button;
    }

    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
}
