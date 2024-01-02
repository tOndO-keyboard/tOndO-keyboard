using UnityEngine;
using UnityEngine.UI;

[RequireComponent(typeof(Toggle))]
[RequireComponent(typeof(UniqueId))]
public class PersistentToggle : MonoBehaviour
{
    private Toggle toggle;
    private UniqueId uniqueId;

    private void Awake()
    {
        toggle = GetComponent<Toggle>();
        uniqueId = GetComponent<UniqueId>();

        bool wasOn = PlayerPrefs.GetInt(uniqueId.uniqueId, 0) == 1;

        if (wasOn != toggle.isOn) toggle.isOn = wasOn;
        
        toggle.onValueChanged.Invoke(toggle.isOn);

        toggle.onValueChanged.AddListener(delegate { ToggleValueChanged(); });
    }

    private void ToggleValueChanged()
    {
        PlayerPrefs.SetInt(uniqueId.uniqueId, toggle.isOn ? 1 : 0);
    }
}
