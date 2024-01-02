using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class EmojisController : MonoBehaviour
{
    [SerializeField]
    private float _updateCooldown = 1;

    private List<EmojiSprite> _sprites = new List<EmojiSprite>();

    private float _timer = 0;
    private bool _requestOnCooldown = false;
    
    private void Awake()
    {
        _sprites.AddRange(GetComponentsInChildren<EmojiSprite>());

        if (EmojiPersister.Instance.Capacity != _sprites.Count)
            EmojiPersister.Instance.Capacity = _sprites.Count;
        EmojiPersister.Instance.Changed += OnPersisterChanged;
    }

    private void Start()
    {
        Repopulate();
    }

    private void Update()
    {
        if (_timer == 0) return;

        _timer -= Time.unscaledDeltaTime;

        _timer = Mathf.Max(0, _timer);
        if (_timer == 0 && _requestOnCooldown)
        {
            _requestOnCooldown = false;
            OnPersisterChanged(EmojiPersister.ChangeType.MostUsed);
        }
    }

    private void OnApplicationPause(bool paused)
    {
        if (paused) EmojiPersister.SaveInstance();
    }

    private void OnDestroy()
    {
        EmojiPersister.SaveInstance();
        EmojiPersister.Instance.Changed -= OnPersisterChanged;
    }

    private void OnPersisterChanged(EmojiPersister.ChangeType change) 
    {
        if (_timer == 0 && change == EmojiPersister.ChangeType.MostUsed) Repopulate();
        else if (change == EmojiPersister.ChangeType.MostUsed) _requestOnCooldown = true;
        _timer = _updateCooldown;
    }

    private void Repopulate()
    {
        var keys = EmojiPersister.Instance.DescendingOrderedKeys.ToList();
        for (int i = 0; i < _sprites.Count; i++)
        {
            var sprite = _sprites[i];
            if (i < keys.Count)
            {
                sprite.Code = keys[i];
                sprite.Image.sprite = EmojiSprite.GetImage(keys[i], sprite.SpriteAtlas);

                ProLock proLock = sprite.GetComponent<ProLock>();
                if (proLock != null)
                {
                    sprite.gameObject.SetActive(proLock.CouldBeEnabled());
                }
                else
                {
                    sprite.gameObject.SetActive(true);
                }

            }
            else
            {
                sprite.gameObject.SetActive(false);
            }
        }
    }
}
