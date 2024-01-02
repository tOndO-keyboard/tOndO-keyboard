using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public abstract class LockOverride : MonoBehaviour
{
    public abstract void LockFunction();

#if UNITY_EDITOR
    private void OnValidate()
    {
        var pLock = GetComponent<ProLock>();
        if (pLock == null) return;
        
        typeof(ProLock).GetField("_override", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance)
                .SetValue(pLock, this);
    }
#endif
}
