using UnityEngine;
using System.Reflection;
using System.Collections.Generic;

public class TouchCreator
{
    static Dictionary<string, FieldInfo> fields;

    object touch;

    public float deltaTime { get { return ((Touch)touch).deltaTime; } set { fields["m_TimeDelta"].SetValue(touch, value); } }
    public int tapCount { get { return ((Touch)touch).tapCount; } set { fields["m_TapCount"].SetValue(touch, value); } }
    public TouchPhase phase { get { return ((Touch)touch).phase; } set { fields["m_Phase"].SetValue(touch, value); } }
    public Vector2 deltaPosition { get { return ((Touch)touch).deltaPosition; } set { fields["m_PositionDelta"].SetValue(touch, value); } }
    public int fingerId { get { return ((Touch)touch).fingerId; } set { fields["m_FingerId"].SetValue(touch, value); } }
    public Vector2 position { get { return ((Touch)touch).position; } set { fields["m_Position"].SetValue(touch, value); } }
    public Vector2 rawPosition { get { return ((Touch)touch).rawPosition; } set { fields["m_RawPosition"].SetValue(touch, value); } }

    public Touch Create()
    {
        return (Touch)touch;
    }

    public TouchCreator()
    {
        touch = new Touch();
    }

    static TouchCreator()
    {
        fields = new Dictionary<string, FieldInfo>();
        foreach (var f in typeof(Touch).GetFields(BindingFlags.Instance | BindingFlags.NonPublic))
        {
            fields.Add(f.Name, f);
        }
    }
}