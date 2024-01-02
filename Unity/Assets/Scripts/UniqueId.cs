using UnityEngine;
using System.Collections.Generic;
using System;

#if UNITY_EDITOR
using UnityEditor;
using UnityEditor.SceneManagement;
#endif

public class UniqueIdentifierAttribute : PropertyAttribute { }

[ExecuteInEditMode]
public class UniqueId : MonoBehaviour
{
    static Dictionary<string, UniqueId> allGuids = new Dictionary<string, UniqueId>();

    [UniqueIdentifier]
    public string uniqueId;


#if UNITY_EDITOR

    private void Start()
    {
        if(Application.isPlaying)
            return;

        bool anotherComponentAlreadyHasThisID = uniqueId != null && 
                                                allGuids.ContainsKey(uniqueId) && 
                                                allGuids[uniqueId] != this;

        if(string.IsNullOrEmpty(uniqueId) || anotherComponentAlreadyHasThisID)
        {
            uniqueId = Guid.NewGuid().ToString();
            EditorUtility.SetDirty(this);
            EditorSceneManager.MarkSceneDirty(gameObject.scene);
        }

        if(!allGuids.ContainsKey(uniqueId))
        {
            allGuids.Add(uniqueId, this);
        }
    }
    
    private void OnDestroy()
    {
        allGuids.Remove(uniqueId);
    }

#endif
}