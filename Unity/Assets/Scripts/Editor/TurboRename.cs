using UnityEngine;
using UnityEditor;
using System;
using System.Reflection;
using System.Collections.Generic;

public enum NumberedMethod
{
    BySelection = 0,
    ByHierarchy = 1
}

[Serializable]
public class TurboRename : EditorWindow
{
    UnityEngine.Object[] SelectedObjects = new UnityEngine.Object[0];
    GameObject[] SelectedGameObjectObjects = new GameObject[0];

    string[] PreviewSelectedObjects = new string[0];

    bool usebasename;
    string basename;
    bool useprefix;
    string prefix;
    bool usesuffix;
    string suffix;

    public NumberedMethod numbermeth;
    bool usenumbered;
    int basenumbered = 0;
    int stepnumbered = 1;

    bool usereplace;
    string replace;
    string replacewith;

    bool useremove;
    string remove;

    bool showselection;
    // Add menu item named "My Window" to the Window menu
    [MenuItem("Tools/Turbo Rename")]
    public static void ShowWindow()
    {
        //Show existing window instance. If one doesn't exist, make one.
        var window = EditorWindow.GetWindow(typeof(TurboRename));
        window.minSize = new Vector2(512, 128);
    }

    #region GUI
    void OnGUI()
    {

        EditorGUILayout.BeginVertical("Box");
        GUILayout.Label("Turbo Rename", EditorStyles.boldLabel);
        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        usebasename = EditorGUILayout.Toggle(usebasename, GUILayout.MaxWidth(16));
        basename = EditorGUILayout.TextField("Base Name: ", basename);
        EditorGUILayout.EndHorizontal();
        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        useprefix = EditorGUILayout.Toggle(useprefix, GUILayout.MaxWidth(16));
        prefix = EditorGUILayout.TextField("Prefix: ", prefix);
        EditorGUILayout.EndHorizontal();
        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        usesuffix = EditorGUILayout.Toggle(usesuffix, GUILayout.MaxWidth(16));
        suffix = EditorGUILayout.TextField("Suffix: ", suffix);
        EditorGUILayout.EndHorizontal();
        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        usenumbered = EditorGUILayout.Toggle(usenumbered, GUILayout.MaxWidth(16));
        EditorGUILayout.PrefixLabel("Numbered: ");
        EditorGUILayout.BeginVertical();
        basenumbered = EditorGUILayout.IntField("Start number: ",basenumbered);
        stepnumbered = EditorGUILayout.IntField("Step: ",stepnumbered);
        numbermeth = (NumberedMethod)EditorGUILayout.EnumPopup(new GUIContent("Number method", "Number by position in selection, or number by hierarchy position. Note: Project files cannot be renamed with the hierarchy method as they are not present in the scene."), numbermeth);
        EditorGUILayout.EndVertical();
        EditorGUILayout.EndHorizontal();


        EditorGUILayout.Space();

        EditorGUILayout.BeginHorizontal();
        usereplace = EditorGUILayout.Toggle(usereplace, GUILayout.MaxWidth(16));
        EditorGUILayout.PrefixLabel("Replace contents: ");
        EditorGUILayout.BeginVertical();

        replace = EditorGUILayout.TextField("Replace: ", replace);
        replacewith = EditorGUILayout.TextField("With: ", replacewith);
        EditorGUILayout.EndVertical();
        EditorGUILayout.EndHorizontal();
        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        useremove = EditorGUILayout.Toggle(useremove, GUILayout.MaxWidth(16));
        remove = EditorGUILayout.TextField("Remove all: ", remove);
        EditorGUILayout.EndHorizontal();

        EditorGUILayout.Space();
        //Rename
        if (GUILayout.Button(new GUIContent("Rename", "Renames selected objects with current settings."))) { Rename();  }
        EditorGUILayout.EndVertical();

        if (SelectedObjects.Length > 0)
        {
            showselection = EditorGUILayout.Foldout(showselection, "Selected objects and preview");
            if (showselection)
            {
                EditorGUILayout.BeginHorizontal();
                EditorGUILayout.BeginVertical("Box");
                GUILayout.Label("Selection", EditorStyles.boldLabel);
                EditorGUILayout.Space();
                for (int i = 0; i < SelectedObjects.Length; i++)
                {
                    EditorGUILayout.LabelField(SelectedObjects[i].name);
                }
                EditorGUILayout.EndVertical();
                EditorGUILayout.BeginVertical("Box");
                GUILayout.Label("Preview", EditorStyles.boldLabel);
                EditorGUILayout.Space();
                for (int i = 0; i < SelectedObjects.Length; i++)
                {
                    EditorGUILayout.LabelField(PreviewSelectedObjects[i]);
                }

                EditorGUILayout.EndVertical();

                EditorGUILayout.EndHorizontal();
            }
        }
        if (GUILayout.Button(new GUIContent("Clear settings", "Renames selected objects with current settings."))) { ClearSettings(); }


    }
    #endregion

    #region Functions
    private void Update()
    {
        SelectedObjects = Selection.objects;
        
        SelectedGameObjectObjects = Selection.gameObjects;

        PreviewSelectedObjects = new string[SelectedObjects.Length];

        for (int i = 0; i < SelectedObjects.Length; i++)
        {
            string str = SelectedObjects[i].name;
            if (usebasename) { str = basename; }
            if (useprefix) { str = prefix + str; }
            if (usesuffix) { str = str + suffix; }

            if (usenumbered && numbermeth == NumberedMethod.BySelection) { str = str + ((basenumbered + (stepnumbered * i)).ToString()); }

            if (useremove && remove != "") { str = str.Replace(remove, ""); }
            if (usereplace && replace != "") { str = str.Replace(replace, replacewith); }

            if (usenumbered && numbermeth == NumberedMethod.ByHierarchy)
            {
                for (int z = 0; z < SelectedGameObjectObjects.Length; z++)
                {
                    if ((UnityEngine.Object)SelectedGameObjectObjects[z] == (UnityEngine.Object)SelectedObjects[i])
                    {
                        str = str + ((basenumbered + (stepnumbered * SelectedGameObjectObjects[z].transform.GetSiblingIndex())).ToString());
                    }
                }
            }

                PreviewSelectedObjects[i] = str;
        }

    }

    void Rename()
    {
        
        for (int i = 0; i < SelectedObjects.Length; i++)
        {
            Undo.RecordObject(SelectedObjects[i], "Rename");
            if (usebasename) { SelectedObjects[i].name = basename; }
            if (useprefix) { SelectedObjects[i].name = prefix + SelectedObjects[i].name; }
            if (usesuffix) { SelectedObjects[i].name = SelectedObjects[i].name + suffix; }

            if (usenumbered && numbermeth == NumberedMethod.BySelection) { SelectedObjects[i].name = SelectedObjects[i].name + ((basenumbered + (stepnumbered * i)).ToString()); }
            
            if (useremove && remove != "") { SelectedObjects[i].name = SelectedObjects[i].name.Replace(remove, ""); }
            if (usereplace && replace != "") { SelectedObjects[i].name = SelectedObjects[i].name.Replace(replace, replacewith); }

            if(AssetDatabase.GetAssetPath(SelectedObjects[i]) != null)
            {
                AssetDatabase.RenameAsset(AssetDatabase.GetAssetPath(SelectedObjects[i]), SelectedObjects[i].name);
            }

        }

        for (int i = 0; i < SelectedGameObjectObjects.Length; i++)
        {
            if (usenumbered && numbermeth == NumberedMethod.ByHierarchy) { SelectedGameObjectObjects[i].name = SelectedGameObjectObjects[i].name + ((basenumbered + (stepnumbered * SelectedGameObjectObjects[i].transform.GetSiblingIndex())).ToString()); }

        }
    }

    void ClearSettings()
    {
        usebasename = false;
        basename = "";
        useprefix = false;
        prefix = "";
        usesuffix = false;
        suffix = "";
        usenumbered = false;
        basenumbered = 0;
        stepnumbered = 1;

        usereplace = false;
        replace = "";
        replacewith = "";

        useremove = false;
        remove = "";
}
    #endregion
    

}