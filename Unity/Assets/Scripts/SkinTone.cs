public enum SkinTone
{
    None = 0,
    Light = 0x1f3fb,
    MediumLight = 0x1f3fc,
    Medium = 0x1f3fd,
    MediumDark = 0x1f3fe,
    Dark = 0x1f3ff
}

public static class SkinTones
{
    public static string AsUtf16(this SkinTone skinTone) => char.ConvertFromUtf32((int)skinTone);
    public static int AsInt32(this SkinTone skinTone) => (int)skinTone;
    public static bool IsSet(this SkinTone skinTone) => skinTone != SkinTone.None;
}
