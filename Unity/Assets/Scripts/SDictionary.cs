using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;


[Serializable]
public abstract class SDictionary<TK, TV> : IDictionary<TK, TV>, ISerializationCallbackReceiver
{
    protected Dictionary<TK, TV> dictionary = new Dictionary<TK, TV>();

    protected abstract List<TK> SerializedKeys { get; set; }
    protected abstract List<TV> SerializedValues { get; set; }

    #region ISerializationCallbackReceiver

    public void OnAfterDeserialize()
    {
        Clear();

        if (SerializedKeys != null && SerializedValues != null && SerializedKeys.Count == SerializedValues.Count)
        {
            for (int i = 0; i < SerializedKeys.Count; ++i)
            {
                Add(SerializedKeys[i], SerializedValues[i]);
            }
        }

        SerializedKeys = null;
        SerializedValues = null;
    }

    public void OnBeforeSerialize()
    {
        SerializedKeys = new List<TK>(Keys);
        SerializedValues = new List<TV>(Values);
    }

    #endregion

    #region IDictionary<TK, TV>

    public ICollection<TK> Keys { get { return dictionary.Keys; } }
    public ICollection<TV> Values { get { return dictionary.Values; } }

    public int Count { get { return dictionary.Count; } }

    public bool IsReadOnly
    {
        get { return ((IDictionary<TK, TV>)dictionary).IsReadOnly; }
    }

    public TV this[TK key]
    {
        get { return dictionary[key]; }
        set { dictionary[key] = value; }
    }

    public void Add(TK key, TV value)
    {
        (dictionary).Add(key, value);
    }

    public bool ContainsKey(TK key)
    {
        return (dictionary).ContainsKey(key);
    }

    public bool Remove(TK key)
    {
        return (dictionary).Remove(key);
    }

    public bool TryGetValue(TK key, out TV value)
    {
        return (dictionary).TryGetValue(key, out value);
    }

    public void Add(KeyValuePair<TK, TV> item)
    {
        ((IDictionary<TK, TV>)dictionary).Add(item);
    }

    public void Clear()
    {
        dictionary.Clear();
    }

    public bool Contains(KeyValuePair<TK, TV> item)
    {
        return ((IDictionary<TK, TV>)dictionary).Contains(item);
    }

    public void CopyTo(KeyValuePair<TK, TV>[] array, int arrayIndex)
    {
        ((IDictionary<TK, TV>)dictionary).CopyTo(array, arrayIndex);
    }

    public bool Remove(KeyValuePair<TK, TV> item)
    {
        return ((IDictionary<TK, TV>)dictionary).Remove(item);
    }

    public IEnumerator<KeyValuePair<TK, TV>> GetEnumerator()
    {
        return (dictionary).GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
        return (dictionary).GetEnumerator();
    }

    #endregion
}
