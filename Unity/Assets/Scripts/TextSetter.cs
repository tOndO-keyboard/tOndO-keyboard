using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(Text))]
public class TextSetter : MonoBehaviour
{
    [SerializeField]
    private Text t;
    private void Awake()
    {
        t = GetComponent<Text>();
    }

    public void setText(Slider s)
    {
        t.text = s.value.ToString();
    }
}
