using UnityEngine;

public class SkinToneButton : KeyboardButton
{
    public enum BehaviourType { Normal, AlwaysAffectBoth, OverrideNoneType }
    [SerializeField]
    private SkinTone _toneToSet = SkinTone.None;
    [SerializeField]
    private SelectedTonesSplitVisualizer _icon = null;

    [SerializeField]
    private BehaviourType _behaviour = BehaviourType.Normal;

    private void Awake() 
    {
        EmojiPersister.Instance.Changed += OnPersistenceChange;
        OnPersistenceChange(EmojiPersister.ChangeType.SkinTone);
    }

    private void OnDestroy() => EmojiPersister.Instance.Changed -= OnPersistenceChange;

    private void OnPersistenceChange(EmojiPersister.ChangeType change)
    {
        if (change != EmojiPersister.ChangeType.SkinTone) return;

        bool isTone1 = EmojiPersister.Instance.SkinTone1 == _toneToSet;
        bool isTone2 = EmojiPersister.Instance.SkinTone2 == _toneToSet;
        _icon.SetSelected(isTone1, isTone2);
    }

    public override void OnKeyTrigger() 
    {
        EmojiPersister.Instance.SkinTone1 = _toneToSet;
        EmojiPersister.Instance.SkinTone2 = _toneToSet;
    }

    public override void OnLongPressTrigger()
    {
        bool mustAffectBoth = _behaviour == BehaviourType.AlwaysAffectBoth;
        bool mustOverrideNone = _behaviour == BehaviourType.OverrideNoneType;
        bool isNonePresent = EmojiPersister.Instance.SkinTone1 == SkinTone.None ||
                EmojiPersister.Instance.SkinTone2 == SkinTone.None;

        if (mustAffectBoth || isNonePresent && mustOverrideNone) OnKeyTrigger();
        else EmojiPersister.Instance.SkinTone2 = _toneToSet;
    }
}
