using UnityEditor;
using UnityEngine;

[CustomEditor(typeof(TondoGlyph))]
public class TondoGlyphEditor : Editor
{
    public override void OnInspectorGUI()
    {
        DrawDefaultInspector();

        TondoGlyph script = (TondoGlyph)target;

        if(script.IsDiacriticizer)
        {
            script.IsLeftDiacriticizer = EditorGUILayout.Toggle( "IsLeftDiacriticizer", script.IsLeftDiacriticizer, new GUILayoutOption[0]);
        }
    }
}