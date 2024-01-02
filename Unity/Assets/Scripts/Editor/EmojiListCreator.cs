using UnityEngine;
using UnityEditor;
using System;
using System.Collections.Generic;
using UnityEngine.UI;
using UnityEngine.U2D;
using System.Collections;
using System.IO;
using System.Linq;

public class EmojiListCreator : EditorWindow
{
    private static readonly string CategorySpritePrefix = "emoji_category_icon_";
    private TextAsset emojiCodesTextListObject;
    private ScrollRect emojiScrollView;
    private GameObject categoryButtonsParent;
    private CategoryContainer containerPrefab;
    private SpriteAtlas emojiSpriteAtlas;
    private string emojiAssetsPath;
    private string unusedEmojiFilesFolderPath;

    [MenuItem("Tools/Emoji List Creator")]
    public static void ShowWindow()
    {
        var window = EditorWindow.GetWindow(typeof(EmojiListCreator));
        window.minSize = new Vector2(512, 128);
    }

    void OnGUI()
    {
        EditorGUILayout.BeginVertical("Box");
        GUILayout.Label("Emoji List Creator", EditorStyles.boldLabel);

        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        EditorGUILayout.LabelField("Emoji Text List");
        emojiCodesTextListObject = (TextAsset)EditorGUILayout.ObjectField(emojiCodesTextListObject, typeof(TextAsset), false);
        EditorGUILayout.EndHorizontal();

        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        EditorGUILayout.LabelField("Emoji Scroll View");
        emojiScrollView = (ScrollRect)EditorGUILayout.ObjectField(emojiScrollView, typeof(ScrollRect), true);
        EditorGUILayout.EndHorizontal();

        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        EditorGUILayout.LabelField("Category Buttons Parent");
        categoryButtonsParent = (GameObject)EditorGUILayout.ObjectField(categoryButtonsParent, typeof(GameObject), true);
        EditorGUILayout.EndHorizontal();

        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        EditorGUILayout.LabelField("Emoji Group prefab");
        containerPrefab = (CategoryContainer)EditorGUILayout.ObjectField(containerPrefab, typeof(CategoryContainer), false);
        EditorGUILayout.EndHorizontal();

        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        EditorGUILayout.LabelField("Emoji Sprite Atlas");
        emojiSpriteAtlas = (SpriteAtlas)EditorGUILayout.ObjectField(emojiSpriteAtlas, typeof(SpriteAtlas), false);
        EditorGUILayout.EndHorizontal();

        EditorGUILayout.Space();
        if (GUILayout.Button(new GUIContent("Start"))) { Start();  }
        EditorGUILayout.EndVertical();

        EditorGUILayout.BeginVertical("Box");
        GUILayout.Label("Emoji Assets Cleaner", EditorStyles.boldLabel);

        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        EditorGUILayout.LabelField("Emoji Assets Path");
        emojiAssetsPath = EditorGUILayout.TextField(emojiAssetsPath);
        EditorGUILayout.EndHorizontal();

        EditorGUILayout.Space();
        EditorGUILayout.BeginHorizontal();
        EditorGUILayout.LabelField("Unused Emoji files folder path");
        unusedEmojiFilesFolderPath = EditorGUILayout.TextField(unusedEmojiFilesFolderPath);
        EditorGUILayout.EndHorizontal();


        EditorGUILayout.Space();
        if (GUILayout.Button(new GUIContent("Clean Emoji folder from unused"))) { Clean(); }
        EditorGUILayout.EndVertical();
    }

    private void Start()
    {
        var meta = new EmojiMeta(emojiCodesTextListObject, true);

        Dictionary<string, CategoryButton> buttons = categoryButtonsParent?
                .GetComponentsInChildren<CategoryButton>()?
                .ToDictionary(b => b.name, b => b);

        string atlasFolder = AssetDatabase.GetAssetPath(emojiSpriteAtlas);
        atlasFolder = atlasFolder.Substring(0, atlasFolder.LastIndexOf("/"));
        foreach (string category in meta.Categories)
        {
            CategoryContainer emojiContainer = Instantiate(containerPrefab, emojiScrollView.content.transform);
            emojiContainer.name = category;
            emojiContainer.ScrollRect = emojiScrollView;

            string categoryIconName = $"{CategorySpritePrefix}{category.Replace(" " , "").ToLower()}";
            string path = atlasFolder + $"/{categoryIconName}.png";
            Sprite sprite = AssetDatabase.LoadAssetAtPath<Sprite>(path);
            
            if (sprite == null) Debug.LogWarning("Couldn't find a category icon for " + category);
            else emojiContainer.Icon = sprite;

            if (buttons != null && buttons.ContainsKey(category)) buttons[category].CategoryContainer = emojiContainer;
            else Debug.LogWarning("No button bound to category " + category);

            foreach(EmojiMeta.Emoji emoji in meta.GetByCategory(category))
            {
                GameObject emojiSpriteGO = new GameObject();
                EmojiSprite emojiSprite = emojiSpriteGO.AddComponent<EmojiSprite>();
                emojiSprite.SpriteAtlas = emojiSpriteAtlas;
                emojiSprite.Code = emoji.Code;
                emojiSprite.ScrollRect = emojiScrollView;
                emojiSprite.DeselectOnPointerExit = true;
                emojiSpriteGO.name = emoji.Code.Replace("#skin", "");
                Button emojiSriteButton = emojiSpriteGO.GetComponent<Button>();
                emojiSriteButton.transition = Selectable.Transition.None;
                emojiSpriteGO.transform.SetParent(emojiContainer.transform);
            }
        }
    }

    private void Clean()
    {
        EmojiSprite[] emojiSprites = emojiScrollView.content.transform.GetComponentsInChildren<EmojiSprite>();
        List<string> usedSpritesNames = new List<string>();
        foreach(EmojiSprite emojiSprite in emojiSprites)
        {
            usedSpritesNames.Add(emojiSprite.GetComponent<Image>().sprite.name.Replace("(Clone)", ""));
        }

        string folderPath = Application.dataPath + Path.DirectorySeparatorChar + emojiAssetsPath;
        string[] emojiSpriteFilePaths = Directory.GetFiles(folderPath);

        foreach(string path in emojiSpriteFilePaths)
        {
            if(path.EndsWith(".png"))
            {
                string fileName = path.Replace(folderPath + Path.DirectorySeparatorChar, "").Replace(".png", "");
                if(!usedSpritesNames.Contains(fileName))
                {
                    Directory.Move(path, unusedEmojiFilesFolderPath + Path.DirectorySeparatorChar + fileName + ".png");
                }
            }
        }
    }
}