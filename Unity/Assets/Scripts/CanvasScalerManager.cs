using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(CanvasScaler))]
public class CanvasScalerManager : MonoBehaviour
{
    public static readonly float DEFAULT_PROPORTION = 1.5f;
    public static readonly float LANDSCAPE_PROPORTION = 2.1f;
    public static readonly float HIDDEN_TOP_BAR_PROPORTION = 1.82f;

    [SerializeField]
    private CanvasScaler canvasScaler;

    private void Update()
    {
        canvasScaler.screenMatchMode = Screen.width > Screen.height ? CanvasScaler.ScreenMatchMode.MatchWidthOrHeight : CanvasScaler.ScreenMatchMode.Expand;
    }
}
