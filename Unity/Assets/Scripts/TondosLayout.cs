using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class TondosLayout : MonoBehaviour
{
    [SerializeField]
    private LayoutType layoutType;
    public LayoutType LayoutType
    {
        get
        {
            return layoutType;
        }
    }
}
