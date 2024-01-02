using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SymbolsTondosController : MonoBehaviour
{
    private void Start()
    {
        StartCoroutine(LateDisable());
        NativeInterface.ConfigurationChanged += OnConfigurationChanged;
    }

    private void OnConfigurationChanged()
    {
        gameObject.SetActive(true);
        if (gameObject.activeInHierarchy)
        {
            StartCoroutine(LateDisable());
        }
    }

    public IEnumerator LateDisable()
    {
        yield return new WaitForEndOfFrame();
        yield return new WaitForEndOfFrame();
        gameObject.SetActive(false);
    }
}
