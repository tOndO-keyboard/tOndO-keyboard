using UnityEngine;

public class NoThumbsController : MonoBehaviour
{
    private NativeInterface nativeInterface;

    [SerializeField]
    private CanvasGroup canvasGroup;

    private void Awake()
    {
        nativeInterface = NativeInterface.Instance;
        NativeInterface.InputViewStarted += OnInputViewStarted;
        setCanvas();
    }

    private void OnInputViewStarted(string precedingCharacter)
    {
        setCanvas();
    }

    private void setCanvas()
    {
        bool active = SettingsManager.Instance.InputType == InputType.NoThumbs;
        canvasGroup.gameObject.SetActive(active);
        canvasGroup.alpha = active ? 1 : 0;
        canvasGroup.interactable = active;
        canvasGroup.blocksRaycasts = active;
    }
}
