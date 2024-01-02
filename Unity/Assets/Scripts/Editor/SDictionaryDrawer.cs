using UnityEngine;
using UnityEditor;


    public abstract class SDictionaryDrawer<TK, TV> : PropertyDrawer
    {
        private const float spaceBetweenProperties = 2f;

        #region Key Control

        //Draws and sets the property only if it contains a valid key for the dictionary
        public abstract void SafeDrawKeyAtIndex(Rect position, SerializedProperty keys, int index, GUIContent label, bool includeChildren);

        //Called when creating a new item since duplicating the last one causes a key conflict
        public abstract void GenerateValidKeyAtIndex(SerializedProperty keys, int index);

        //Checks if the property array keys contains the provided key
        public abstract bool IsValidKey(SerializedProperty keys, TK key);

        #endregion

        #region PropertyDrawer

        public override void OnGUI(Rect position, SerializedProperty property, GUIContent label)
        {
            SerializedProperty keys = property.FindPropertyRelative("serializedKeys");
            SerializedProperty values = property.FindPropertyRelative("serializedValues");

            float baseHeight = base.GetPropertyHeight(property, label);

            position.height = baseHeight;

            EditorGUI.PropertyField(position, property, label, false);

            if (keys == null || values == null || keys.arraySize != values.arraySize)
            {
                return;
            }

            int size = keys.arraySize;

            if (property.isExpanded)
            {
                position.y += baseHeight + spaceBetweenProperties;

                ++EditorGUI.indentLevel;

                size = EditorGUI.DelayedIntField(position, "Size", size);

                --EditorGUI.indentLevel;

                if (size != keys.arraySize)
                {
                    while (keys.arraySize < size)
                    {
                        keys.InsertArrayElementAtIndex(keys.arraySize);
                        GenerateValidKeyAtIndex(keys, keys.arraySize - 1);
                    }

                    keys.arraySize = size;
                    values.arraySize = size;
                }

                position.y += EditorGUI.GetPropertyHeight(keys.FindPropertyRelative("Array.size")) + spaceBetweenProperties;

                for (int i = 0; i < size; ++i)
                {
                    SerializedProperty key = keys.GetArrayElementAtIndex(i);
                    SerializedProperty value = values.GetArrayElementAtIndex(i);

                    ++EditorGUI.indentLevel;

                    SafeDrawKeyAtIndex(position, keys, i, new GUIContent("Element " + i + " (Key)"), true);
                    position.y += EditorGUI.GetPropertyHeight(key, label) + spaceBetweenProperties;

                    EditorGUI.PropertyField(position, value, new GUIContent("Element " + i + " (Value)"), true);
                    position.y += EditorGUI.GetPropertyHeight(value, label) + spaceBetweenProperties;

                    --EditorGUI.indentLevel;
                }
            }
        }

        public override float GetPropertyHeight(SerializedProperty property, GUIContent label)
        {
            SerializedProperty keys = property.FindPropertyRelative("serializedKeys");
            SerializedProperty values = property.FindPropertyRelative("serializedValues");

            float baseHeight = base.GetPropertyHeight(property, label);

            if (keys == null || values == null || keys.arraySize != values.arraySize)
            {
                return baseHeight;
            }

            float totalHeight = baseHeight + spaceBetweenProperties;

            if (property.isExpanded)
            {
                totalHeight += EditorGUI.GetPropertyHeight(keys.FindPropertyRelative("Array.size"));

                for (int i = 0; i < keys.arraySize; ++i)
                {
                    totalHeight += spaceBetweenProperties * 2
                      + EditorGUI.GetPropertyHeight(keys.GetArrayElementAtIndex(i), label, true)
                      + EditorGUI.GetPropertyHeight(values.GetArrayElementAtIndex(i), label, true);
                }
            }

            return totalHeight;
        }

        #endregion
    }