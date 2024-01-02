using UnityEditor;
using UnityEngine;
 
[CustomEditor(typeof(SettingsManager)), CanEditMultipleObjects]
public class SettingsManagerEditor : Editor
{

    public SerializedProperty charCustomizationLongStringProp;
    private Vector2 charCustomizationScrollPos;

    public SerializedProperty diacriticsCustomizationLongStringProp;
    private Vector2 diacriticsCustomizationScrollPos;

    void OnEnable()
    {
        charCustomizationLongStringProp = serializedObject.FindProperty("characterCustomizationString");

        diacriticsCustomizationLongStringProp = serializedObject.FindProperty("diacriticsCustomizationString");
    }

    public override void OnInspectorGUI()
    {
        DrawDefaultInspector();
        serializedObject.Update();

        GUILayout.Label("Character Customization String");
        charCustomizationScrollPos = GUILayout.BeginScrollView(charCustomizationScrollPos, GUILayout.Height(200));
        charCustomizationLongStringProp.stringValue = EditorGUILayout.TextArea(charCustomizationLongStringProp.stringValue, GUILayout.ExpandHeight(true));
        GUILayout.EndScrollView();

        GUILayout.Label("Diacritics Customization String");
        diacriticsCustomizationScrollPos = GUILayout.BeginScrollView(diacriticsCustomizationScrollPos, GUILayout.Height(200));
        diacriticsCustomizationLongStringProp.stringValue = EditorGUILayout.TextArea(diacriticsCustomizationLongStringProp.stringValue, GUILayout.ExpandHeight(true));
        GUILayout.EndScrollView();

        serializedObject.ApplyModifiedProperties();
    }
}
