using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using UnityEngine;

[Serializable]
public class MacrosPersister
{
    private const string FILE_NAME = "MacrosPersister";

    private static MacrosPersister _instance = null;

    public static string FilePath =>  $"{Application.persistentDataPath}/{FILE_NAME}";

    public static MacrosPersister Instance 
    { 
        get
        {
            if (_instance == null)
            {
                string filePath = FilePath;
                string json = null;
                if (File.Exists(filePath)) json = File.ReadAllText(filePath);
                if (string.IsNullOrEmpty(json)) 
                {
                    Localization l = Localization.Instance;
                    _instance = new MacrosPersister();
                    if (l.IsInitialized)
                    {
                        _instance.AddTutorialMacro(l.Localize(Localization.MACRO_TUTORIAL_PRO));
                        _instance.AddTutorialMacro(l.Localize(Localization.MACRO_TUTORIAL_RIGHT));
                        _instance.AddTutorialMacro(l.Localize(Localization.MACRO_TUTORIAL_LEFT));
                    }
                }
                else 
                {
                    _instance = JsonUtility.FromJson<MacrosPersister>(json);
                    if (_instance.Sanitize()) DebugLogger.Log("Macro persister was sanitized", DebugLogger.LogType.WARNING);
                }
            }
            return _instance;
        }
    }

    public static void SaveInstance() => Instance.Save();

    public event Action Changed;

    [SerializeField]
    private List<int> _usages = new List<int>();

    [SerializeField]
    private List<string> _macros = new List<string>();

    [SerializeField]
    private List<bool> _areTutorial = new List<bool>();

    [SerializeField]
    private int _capacity = 20;

    private int _batchingChanges = 0;

    public ICollection<string> Keys => _macros.AsReadOnly();

    public ICollection<int> Values => _usages.AsReadOnly();

    public ICollection<string> AscendingOrderedKeys => Keys.OrderBy(k => this[k]).ToArray();
    public ICollection<string> DescendingOrderedKeys => Keys.OrderByDescending(k => this[k]).ToArray();

    public int Count => _macros.Count;

    public int Capacity
    {
        get => _capacity;
        set
        {
            if (value < Count)
            {
                StartChangeBatch();
                foreach (var k in DescendingOrderedKeys.Skip(value).ToList())
                    RemoveMacro(k);
                EndChangeBatch();
            }
            _capacity = value;
        }
    }

    public bool IsReadOnly => false;

    public int this[string key] 
    { 
        get
        {
            int index = _macros.IndexOf(key);
            if (index < 0) throw new KeyNotFoundException("Key " + key + " not found");
            return _usages[index];
        } 

        set
        {
            //StartChangeBatch();
            int index = _macros.IndexOf(key);
            if (index < 0) throw new KeyNotFoundException("Key " + key + " not found");
            _usages[index] = value;
            //EndChangeBatch();
        } 
    }

    private bool Equalize<T>(List<T> list, T fillValue)
    {
        if (list.Count == _macros.Count) return false;
        if (list.Count > _macros.Count) 
            list.RemoveRange(_macros.Count, list.Count - _macros.Count);
        else list.AddRange(Enumerable.Repeat(fillValue, _macros.Count - list.Count));
        return true;
    }

    private bool Sanitize()
    {
        bool sanitized = Equalize(_usages, 1);
        sanitized |= Equalize(_areTutorial, false);

        return sanitized;
    }

    private void StartChangeBatch() => _batchingChanges++;

    private void EndChangeBatch()
    {
        _batchingChanges--;
        if (_batchingChanges < 0) 
            throw new InvalidOperationException("Called EndChangeBatch without a corresponding StartChangeBatch()");
        if (_batchingChanges == 0) Changed?.Invoke();
    }

    private void AddMacro(string macro, int value, bool isTutorial, int index = 0)
    {
        StartChangeBatch();
        if (Count + 1 > _capacity)
            throw new InvalidOperationException("Reached capacity");
        _macros.Insert(index, macro);
        _usages.Insert(index, value);
        _areTutorial.Insert(index, isTutorial);
        EndChangeBatch();
    }

    public void AddMacro(string macro) => AddMacro(macro, 1);

    public void AddMacro(string key, int value) => AddMacro(key, value, false);

    public void AddTutorialMacro(string key) => AddMacro(key, 1, true);

    public bool RemoveMacro(string key)
    {
        if (!_macros.Contains(key)) return false;
        StartChangeBatch();
        int index = _macros.IndexOf(key);
        _macros.RemoveAt(index);
        _usages.RemoveAt(index);
        _areTutorial.RemoveAt(index);
        EndChangeBatch();
        return true;
    }

    public bool ContainsMacro(string key) => _macros.Contains(key);

    public bool TryGetMacro(string key, out int value)
    {
        value = -1;
        if (!ContainsMacro(key)) return false;
        value = this[key];
        return true;
    }

    public bool IsTutorial(string macro)
    {
        int index = _macros.IndexOf(macro);
        return index >= 0 && _areTutorial[index];
    }

    public void AddMostUsed(KeyValuePair<string, int> item) => AddMacro(item.Key, item.Value);

    public void ClearMostUsed()
    {
        StartChangeBatch();
        _macros.Clear();
        _usages.Clear();
        _areTutorial.Clear();
        EndChangeBatch();
    }

    public bool ContainsMacro(KeyValuePair<string, int> item) => 
        TryGetMacro(item.Key, out int v) && v == item.Value;

    public void CopyMacrosTo(KeyValuePair<string, int>[] array, int arrayIndex)
    {
        if (array == null) throw new ArgumentNullException("array");
        if (arrayIndex < 0) throw new ArgumentOutOfRangeException("arrayIndex");
        if (array.Rank > 1) throw new ArgumentException("array is multidimensional");
        if (array.Length - arrayIndex < Count) throw new ArgumentException("array has not enought space");

        for(int i = 0; i < Count; i++)
            array[i + arrayIndex] = new KeyValuePair<string, int>(_macros[i], _usages[i]);
    }

    public void Save() => File.WriteAllText(FilePath, JsonUtility.ToJson(this));

    public bool RemoveMacro(KeyValuePair<string, int> item) => ContainsMacro(item) && RemoveMacro(item.Key);
}
