using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

public class EmojiMeta : IEnumerable<EmojiMeta.Emoji>
{
    public class Emoji
    {
        public readonly int Id;
        public readonly string Code;
        public readonly string Category;

        public Emoji(int id, string code, string category)
        {
            if (code == null) throw new ArgumentNullException("code");
            if (category == null) throw new ArgumentNullException("category");

            Id = id;
            Code = code;
            Category = category;
        }

        public override bool Equals(object obj)
        {
            return obj != null && obj is Emoji other && 
                    Id == other.Id && Code == other.Code &&
                    Category == other.Category;
        }

        public override int GetHashCode() => (Id, Code, Category).GetHashCode();

        public static bool operator ==(Emoji e1, Emoji e2)
        {
            return ReferenceEquals(e1, null) && ReferenceEquals(e2, null) || 
                    !ReferenceEquals(e1, null) && e1.Equals(e2);
        }

        public static bool operator !=(Emoji e1, Emoji e2) => !(e1 == e2);
    }

    private static Emoji CreateEmoji(string[] fieldsHeaders, string[] tokens)
    {
        if (tokens.Length < fieldsHeaders.Length) throw new ArgumentException("Incompatible CSV file, missing required fields");
        
        int id = -1;
        string code = null;
        string category = null;

        for(int i = 0; i < fieldsHeaders.Length; i++)
        {
            string header = fieldsHeaders[i];
            switch (header) 
            {
                case "ID": id = int.Parse(tokens[i]); break;
                case "Code": code = tokens[i]; break;
                case "Category": category = tokens[i]; break;
                default: throw new ArgumentException("Unknown header " + header);
            }
        }

        return new Emoji(id, code, category);
    }

    private readonly Dictionary<int, Emoji> _emojisById = new Dictionary<int, Emoji>();
    private readonly Dictionary<string, Emoji> _emojisByCode = new Dictionary<string, Emoji>();
    private readonly Dictionary<string, List<Emoji>> _emojisByCategory = new Dictionary<string, List<Emoji>>();

    public Emoji this[string code] => _emojisByCode[code];
    public Emoji this[int id] => _emojisById[id];

    public IEnumerable<string> Codes => _emojisByCode.Keys;
    public IEnumerable<string> Categories => _emojisByCategory.Keys;

    public EmojiMeta(string csvContent, bool useHeaders)
    {
        if (csvContent == null) throw new ArgumentNullException("source csv");

        string[] lines = csvContent.Split(new [] { '\n', '\r' }, StringSplitOptions.RemoveEmptyEntries);
        string[] fields = { "ID", "Code", "Category" };

        for(int i = 0; i < lines.Length; i++)
        {
            string line = lines[i];
            string[] tokens = line.Split(new [] { ',' }, StringSplitOptions.RemoveEmptyEntries);
            
            if (i == 0 && useHeaders)
            {
                fields = tokens;
                continue;
            }

            Emoji emoji = CreateEmoji(fields, tokens);

            if (_emojisById.ContainsKey(emoji.Id)) 
                throw new InvalidOperationException("Duplicate emoji id " + emoji.Id);
            if (_emojisByCode.ContainsKey(emoji.Code))
                throw new InvalidOperationException("Duplicate emoji code " + emoji.Code);
            if (!_emojisByCategory.ContainsKey(emoji.Category))
                _emojisByCategory.Add(emoji.Category, new List<Emoji>());
            
            _emojisById.Add(emoji.Id, emoji);
            _emojisByCode.Add(emoji.Code, emoji);
            _emojisByCategory[emoji.Category].Add(emoji);
        }
    }

    public EmojiMeta(string csvContent) : this(csvContent, false) { }

    public EmojiMeta(TextAsset asset, bool useHeaders) : this(asset == null ? null : asset.text, useHeaders) { }

    public EmojiMeta(TextAsset asset) : this(asset, false) { }

    public bool TryGetByCode(string code, out Emoji emoji) => _emojisByCode.TryGetValue(code, out emoji);
    public bool TryGetById(int id, out Emoji emoji) => _emojisById.TryGetValue(id, out emoji);

    public IEnumerable<Emoji> GetByCategory(string category) 
    {
        if (!_emojisByCategory.ContainsKey(category)) return new List<Emoji>();
        return _emojisByCategory[category].AsReadOnly();
    }

    public IEnumerator<Emoji> GetEnumerator() => _emojisById.Values.GetEnumerator();

    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
}