using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(Slider))]
[RequireComponent(typeof(UniqueId))]
public class PersistentSlider : MonoBehaviour
{
    private Slider slider;
    private UniqueId uniqueId;

    private void Awake()
    {
        slider = GetComponent<Slider>();
        uniqueId = GetComponent<UniqueId>();

        float prevValue = PlayerPrefs.GetFloat(uniqueId.uniqueId, 0);

        if (prevValue != slider.value) slider.value = prevValue;

        slider.onValueChanged.Invoke(slider.value);

        slider.onValueChanged.AddListener(delegate { ToggleValueChanged(); });
    }

    private void ToggleValueChanged()
    {
        PlayerPrefs.SetFloat(uniqueId.uniqueId, slider.value);
    }
}
