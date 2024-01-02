using System;
using UnityEngine;
using UnityEditor;

    public class SDictionaryStringValueDrawer<TV> : SDictionaryDrawer<string, TV>
    {
        public override void GenerateValidKeyAtIndex(SerializedProperty keys, int index)
        {
            keys.GetArrayElementAtIndex(index).stringValue = Guid.NewGuid().ToString();
        }

        public override void SafeDrawKeyAtIndex(Rect position, SerializedProperty keys, int index, GUIContent label, bool includeChildren)
        {
            EditorGUI.BeginChangeCheck();

            SerializedProperty property = keys.GetArrayElementAtIndex(index);

            string key = EditorGUI.DelayedTextField(position, label, property.stringValue);

            if (EditorGUI.EndChangeCheck() && IsValidKey(keys, key))
            {
                property.stringValue = key;
            }
        }

        public override bool IsValidKey(SerializedProperty keys, string key)
        {
            for (int i = 0; i < keys.arraySize; ++i)
            {
                if (keys.GetArrayElementAtIndex(i).stringValue.Equals(key))
                {
                    return false;
                }
            }

            return true;
        }
    }