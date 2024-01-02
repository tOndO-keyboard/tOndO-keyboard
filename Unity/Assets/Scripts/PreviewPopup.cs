using System.Collections;
using System.Collections.Generic;
using TMPro;
using UnityEngine;
using UnityEngine.UI;

public class PreviewPopup : MonoBehaviour
{
    [SerializeField]
    private TMP_Text label;

    [SerializeField]
    private Image labelImage;

    public void SetLabel(string s, int fontSize)
    {
        label.enabled = true;
        labelImage.enabled = false;
        label.text = s;
        label.fontSize = fontSize;
    }

    public void SetlabelImage(Sprite s)
    {
        label.enabled = false;
        labelImage.enabled = true;
        labelImage.sprite = s;
    }
}
