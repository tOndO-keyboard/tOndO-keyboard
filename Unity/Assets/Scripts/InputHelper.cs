using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class InputHelper : LazySingleIstanceMonoBehaviour<InputHelper>
{
    private static TouchCreator lastFakeTouch;

    public static List<Touch> GetTouches()
    {
        List<Touch> touches = new List<Touch>();
        touches.AddRange(Input.touches);
#if UNITY_EDITOR
        if (lastFakeTouch == null) lastFakeTouch = new TouchCreator();
        if (Input.GetMouseButtonDown(0))
        {
            lastFakeTouch.phase = TouchPhase.Began;
            lastFakeTouch.deltaPosition = new Vector2(0, 0);
            lastFakeTouch.position = new Vector2(Input.mousePosition.x, Input.mousePosition.y);
            lastFakeTouch.fingerId = 0;
        }
        else if (Input.GetMouseButtonUp(0))
        {
            lastFakeTouch.phase = TouchPhase.Ended;
            Vector2 newPosition = new Vector2(Input.mousePosition.x, Input.mousePosition.y);
            lastFakeTouch.deltaPosition = newPosition - lastFakeTouch.position;
            lastFakeTouch.position = newPosition;
            lastFakeTouch.fingerId = 0;
        }
        else if (Input.GetMouseButton(0))
        {
            lastFakeTouch.phase = TouchPhase.Moved;
            Vector2 newPosition = new Vector2(Input.mousePosition.x, Input.mousePosition.y);
            lastFakeTouch.deltaPosition = newPosition - lastFakeTouch.position;
            lastFakeTouch.position = newPosition;
            lastFakeTouch.fingerId = 0;
        }
        else
        {
            lastFakeTouch = null;
        }
        if (lastFakeTouch != null) touches.Add(lastFakeTouch.Create());
#endif
        return touches;
    }

}