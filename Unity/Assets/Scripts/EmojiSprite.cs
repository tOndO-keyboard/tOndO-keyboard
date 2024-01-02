using System;
using System.Text;
using UnityEngine;
using UnityEngine.U2D;
using UnityEngine.UI;

[RequireComponent(typeof(Image))]
[RequireComponent(typeof(Button))]
public class EmojiSprite : ScrollRectKeyboardButton
{
    private static readonly string EmojiSpritesNamePrefix = "emoji_u";
    private static readonly string VariantSelectorCode = "_FE0F";
    private static readonly string DefaultSkinToneCode = "_1f3fb";
    private static readonly string SkinToken1 = "#skin_1";
    private static readonly string SkinToken2 = "#skin_2";

    private static Sprite GetSpriteFromSource(string name, SpriteAtlas atlas)
    {
        Sprite s = atlas.GetSprite(name);
#if UNITY_EDITOR
        if (!Application.isPlaying) 
        {
            string atlasFolder = UnityEditor.AssetDatabase.GetAssetPath(atlas);
            atlasFolder = atlasFolder.Substring(0, atlasFolder.LastIndexOf("/"));
            s = UnityEditor.AssetDatabase.LoadAssetAtPath<Sprite>($"{atlasFolder}/{name}.png");
        }
#endif
        return s;
    }

    public static Sprite GetImage(string Code, SpriteAtlas SpriteAtlas)
    {
        //_FE0F is there just to tell the system that this emoji should be presented as an image but noto-emoji names never uses it
        string c = EmojiSpritesNamePrefix + Code.Replace("U+", "")
                .Replace(" ", "_")
                .Replace(VariantSelectorCode, "")
                .ToLower();
        SpriteAtlas atlas = SpriteAtlas;

        string noSkin = c.Replace($"_{SkinToken1}", "").Replace($"_{SkinToken2}", "");

        Sprite s = GetSpriteFromSource(noSkin, atlas);

        if (s == null) 
        {
            string withSkin = c.Replace(SkinToken1, DefaultSkinToneCode).Replace(SkinToken2, DefaultSkinToneCode);
            s = GetSpriteFromSource(withSkin, atlas);
        }

        return s;
    }

    [SerializeField]
    public SpriteAtlas SpriteAtlas;

    [SerializeField]
    public string Code;

    private Image image;
    public Image Image
    {
        get
        {
            if (image == null)
            {
                image = GetComponent<Image>();
            }
            return image;
        }
    }

    public bool DeselectOnPointerExit
    {
        set
        {
            deselectOnPointerExit = value;
        }
    }

    private void OnEnable()
    {
        if(String.IsNullOrEmpty(Code))
        {
            SetActive(false);
        }
    }

    public override void OnKeyTrigger()
    {
        string[] codes = Code.Split(' ');
        StringBuilder utf16Code = new StringBuilder();
        foreach(string code in codes)
        {
            int hexCode = 0;
            if (code == SkinToken1) hexCode = EmojiPersister.Instance.SkinTone1.AsInt32();
            else if (code == SkinToken2) hexCode = EmojiPersister.Instance.SkinTone2.AsInt32();
            else hexCode = Int32.Parse(code.Replace("U+", ""), System.Globalization.NumberStyles.HexNumber);

            if (hexCode != 0) utf16Code.Append(char.ConvertFromUtf32(hexCode));
        }

        EmojiPersister repo = EmojiPersister.Instance;
        if (repo.ContainsMostUsed(Code)) repo[Code]++;
        else repo.AddMostUsed(Code);

        NativeInterface.CommitEmoji(utf16Code.ToString());
    }

    private string previousCode;

    void OnValidate()
    {
        if (!string.IsNullOrEmpty(Code) && !Code.Equals(previousCode))
        {
            previousCode = Code;
            Image.sprite = GetImage(Code, SpriteAtlas);
        }
    }
}
