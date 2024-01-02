using System;
using UnityEngine;
using UnityEditor;


    // Warning: since it's not possible to constraint a generic type to enum, this one is a little bit loose
    public class SDictionaryEnumValueDrawer<TE, TV> : SDictionaryDrawer<int, TV> where TE : struct, IConvertible
    {
        public override void GenerateValidKeyAtIndex(SerializedProperty keys, int index)
        {
            keys.GetArrayElementAtIndex(index).enumValueIndex = 0;
        }

        public override void SafeDrawKeyAtIndex(Rect position, SerializedProperty keys, int index, GUIContent label, bool includeChildren)
        {
            EditorGUI.BeginChangeCheck();

            SerializedProperty property = keys.GetArrayElementAtIndex(index);

            int key = EditorGUI.Popup(position, property.enumValueIndex, property.enumDisplayNames);

            if (EditorGUI.EndChangeCheck() && IsValidKey(keys, key))
            {
                property.enumValueIndex = Convert.ToInt32(key);
            }
        }

        public override bool IsValidKey(SerializedProperty keys, int key)
        {
            for (int i = 0; i < keys.arraySize; ++i)
            {
                if (keys.GetArrayElementAtIndex(i).enumValueIndex == key)
                {
                    return false;
                }
            }

            return true;
        }
    }