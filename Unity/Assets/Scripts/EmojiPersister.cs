using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using UnityEngine;

[Serializable]
public class EmojiPersister
{
    public enum ChangeType { None, SkinTone, MostUsed }

    private const string FILE_NAME = "EmojiPersister";

    private static EmojiPersister _instance = null;

    public static string FilePath =>  $"{Application.persistentDataPath}/{FILE_NAME}";

    public static EmojiPersister Instance 
    { 
        get
        {
            if (_instance == null)
            {
                string filePath = FilePath;
                string json = null;
                if (File.Exists(filePath)) json = File.ReadAllText(filePath);
                if (string.IsNullOrEmpty(json)) _instance = new EmojiPersister();
                else _instance = JsonUtility.FromJson<EmojiPersister>(json);
            }
            return _instance;
        }
    }

    public static void SaveInstance() => Instance.Save();

    public event Action<ChangeType> Changed;

    [SerializeField]
    private List<int> _usages = new List<int>();

    [SerializeField]
    private List<string> _emojis = new List<string>();

    [SerializeField]
    private SkinTone _skinTone1 = SkinTone.None;
    [SerializeField]
    private SkinTone _skinTone2 = SkinTone.None;

    [SerializeField]
    private int _capacity = 10;

    private int _batchingChanges = 0;

    public ICollection<string> Keys => _emojis.AsReadOnly();

    public ICollection<int> Values => _usages.AsReadOnly();

    public ICollection<string> AscendingOrderedKeys => Keys.OrderBy(k => this[k]).ToArray();
    public ICollection<string> DescendingOrderedKeys => Keys.OrderByDescending(k => this[k]).ToArray();

    public int Count => _emojis.Count;

    public SkinTone SkinTone1 
    {
        get => _skinTone1;
        set
        {
            _skinTone1 = value;
            Changed?.Invoke(ChangeType.SkinTone);
        }
    }

    public SkinTone SkinTone2
    {
        get => _skinTone2;
        set
        {
            _skinTone2 = value;
            Changed?.Invoke(ChangeType.SkinTone);
        }
    }

    public (SkinTone Tone1, SkinTone Tone2) SkinTones => (_skinTone1, _skinTone2);

    public int Capacity
    {
        get => _capacity;
        set
        {
            if (value < Count)
            {
                StartMostUsedChangeBatch();
                foreach (var k in DescendingOrderedKeys.Skip(value).ToList())
                    RemoveMostUsed(k);
                EndMostUsedChangeBatch();
            }
            _capacity = value;
        }
    }

    public bool IsReadOnly => false;

    public int this[string key] 
    { 
        get
        {
            int index = _emojis.IndexOf(key);
            if (index < 0) throw new KeyNotFoundException("Key " + key + " not found");
            return _usages[index];
        } 

        set
        {
            int index = _emojis.IndexOf(key);
            if (index < 0) throw new KeyNotFoundException("Key " + key + " not found");
            StartMostUsedChangeBatch();
            _usages[index] = value;
            EndMostUsedChangeBatch();
        } 
    }

    private EmojiPersister() : this(10) { }
    private EmojiPersister(int capacity) : this(capacity, SkinTone.None) { }
    private EmojiPersister(int capacity, SkinTone tones) : this(capacity, tones, tones) { }
    private EmojiPersister(int capacity, SkinTone tone1, SkinTone tone2)
    {
        _capacity = capacity;
        _skinTone1 = tone1;
        _skinTone2 = tone2;
    }

    private void StartMostUsedChangeBatch() => _batchingChanges++;

    private void EndMostUsedChangeBatch()
    {
        _batchingChanges--;
        if (_batchingChanges < 0) 
            throw new InvalidOperationException("Called EndChangeBatch without a corresponding StartChangeBatch()");
        if (_batchingChanges == 0) Changed?.Invoke(ChangeType.MostUsed);
    }

    public void AddMostUsed(string key) => AddMostUsed(key, 1);

    public void AddMostUsed(string key, int value)
    {
        StartMostUsedChangeBatch();
        if (Count + 1 > _capacity)
            RemoveMostUsed(DescendingOrderedKeys.Last());
        _emojis.Add(key);
        _usages.Add(value);
        EndMostUsedChangeBatch();
    }

    public bool ContainsMostUsed(string key) => _emojis.Contains(key);

    public bool RemoveMostUsed(string key)
    {
        StartMostUsedChangeBatch();
        if (!_emojis.Contains(key)) return false;
        int index = _emojis.IndexOf(key);
        _emojis.RemoveAt(index);
        _usages.RemoveAt(index);
        EndMostUsedChangeBatch();
        return true;
    }

    public bool TryGetMostUsedUses(string key, out int value)
    {
        value = -1;
        if (!ContainsMostUsed(key)) return false;
        value = this[key];
        return true;
    }

    public void AddMostUsed(KeyValuePair<string, int> item) => AddMostUsed(item.Key, item.Value);

    public void ClearMostUsed()
    {
        StartMostUsedChangeBatch();
        _emojis.Clear();
        _usages.Clear();
        EndMostUsedChangeBatch();
    }

    public bool ContainsMostUsed(KeyValuePair<string, int> item) => 
        TryGetMostUsedUses(item.Key, out int v) && v == item.Value;

    public void CopyMostUsedTo(KeyValuePair<string, int>[] array, int arrayIndex)
    {
        if (array == null) throw new ArgumentNullException("array");
        if (arrayIndex < 0) throw new ArgumentOutOfRangeException("arrayIndex");
        if (array.Rank > 1) throw new ArgumentException("array is multidimensional");
        if (array.Length - arrayIndex < Count) throw new ArgumentException("array has not enought space");

        for(int i = 0; i < Count; i++)
            array[i + arrayIndex] = new KeyValuePair<string, int>(_emojis[i], _usages[i]);
    }

    public void Save() => File.WriteAllText(FilePath, JsonUtility.ToJson(this));

    public bool RemoveMostUsed(KeyValuePair<string, int> item) => ContainsMostUsed(item) && RemoveMostUsed(item.Key);
}
