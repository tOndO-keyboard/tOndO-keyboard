using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Events;
using UnityEngine.EventSystems;

[RequireComponent(typeof(RectTransform))]
public class SwipeAreaController : MonoBehaviour
{
    public enum Direction
    {
        Up,
        Right,
        Down,
        Left
    };
    public bool TouchStartedFromThisArea { get; private set; }

    [SerializeField]
    private int priority = 0;
    public int Priority
    {
        get
        {
            return priority;
        }
    }

    [SerializeField]
    private Direction direction;
    public Direction SwipeDirection
    {
        get
        {
            return direction;
        }
    }

    [SerializeField]
    private float maxSwipeTime = 0.5f;

    [SerializeField]
    private float minSwipeDistance = 250.0f;

    [SerializeField]
    private float maxSwipeDeviation = 50.0f;

    [SerializeField]
    private bool checkAnalogSticksSelection = false;

    [SerializeField]
    private bool checkOtherSwipeAreaSelection = false;

    [SerializeField]
    private GameObject enableIfThisGameObjectIsActive;

    [SerializeField]
    private UnityEvent onSwipeDetected;

    private Vector2 firstPressPos;
    private Vector2 secondPressPos;
    private bool cancelled = false;
    private float firstPressTime;

    private RectTransform _rectTransform;
    private RectTransform RectTransform
    {
        get
        {
            if(_rectTransform == null)
            {
                _rectTransform = GetComponent<RectTransform>();
            }
            return _rectTransform;
        }
    }

    private AnalogStick[] _sticks;
    private AnalogStick[] Sticks
    {
        get
        {
            if(_sticks == null)
            {
                _sticks = FindObjectsOfType<AnalogStick>();
            }

            return _sticks;
        }
    }

    private SwipeAreaController[] _swipeControllers;
    private SwipeAreaController[] SwipeControllers
    {
        get
        {
            if (_swipeControllers == null)
            {
                List<SwipeAreaController> swipeControllersList = new List<SwipeAreaController>(FindObjectsOfType<SwipeAreaController>());
                swipeControllersList.Remove(this);
                _swipeControllers = swipeControllersList.ToArray();
            }

            return _swipeControllers;
        }
    }

    private NativeInterface _nativeInterface;
    private NativeInterface NativeInterface
    {
        get
        {
            if (_nativeInterface == null)
            {
                _nativeInterface = NativeInterface.Instance;
            }
            return _nativeInterface;
        }
    }

    private CanvasGroup _canvasGroup;

    private bool CanvasGroupWasSearched = false;
    private CanvasGroup CanvasGroup
    {
        get
        {
            if(_canvasGroup == null && !CanvasGroupWasSearched)
            {
                _canvasGroup = GetComponent<CanvasGroup>();
            }

            if (_canvasGroup == null && !CanvasGroupWasSearched)
            {
                _canvasGroup = GetComponentInParent<CanvasGroup>();
            }

            CanvasGroupWasSearched = true;
            return _canvasGroup;
        }
    }

    private void Update()
    {
        if(enableIfThisGameObjectIsActive != null && !enableIfThisGameObjectIsActive.activeInHierarchy)
        {
            return;
        }

        if (checkAnalogSticksSelection)
        {
            foreach (AnalogStick stick in Sticks)
            {
                if (stick.Selected)
                {
                    cancelled = true;
                    return;
                }
            }
        }

        if (checkOtherSwipeAreaSelection)
        {
            foreach (SwipeAreaController swipeControllers in SwipeControllers)
            {
                if (swipeControllers.TouchStartedFromThisArea && 
                    direction == swipeControllers.SwipeDirection && 
                    swipeControllers.Priority > this.Priority)
                {
                    cancelled = true;
                    return;
                }
            }
        }

        if(CanvasGroup != null && !CanvasGroup.interactable)
        {
            return;
        }

#if UNITY_EDITOR
        if (InputHelper.GetTouches().Count > 0)
        {
            Touch touch = InputHelper.GetTouches()[0];
#else
        if (Input.touches.Length > 0)
        {
            Touch touch = Input.GetTouch(0);
#endif

            checkTouchIsInsideRect();

            if (touch.phase == TouchPhase.Began && TouchStartedFromThisArea)
            {
                firstPressPos = new Vector2(touch.position.x, touch.position.y);
                firstPressTime = Time.time;
                cancelled = false;
            }
            else if (touch.phase == TouchPhase.Moved && !firstPressPos.Equals(Vector2.zero) && !cancelled)
            {
                secondPressPos = new Vector2(touch.position.x, touch.position.y);
                if (    Time.time - firstPressTime < maxSwipeTime &&
                        Vector2.Distance(firstPressPos, secondPressPos) > minSwipeDistance)
                {
                    if (isSwipeInTheRightDirection(firstPressPos, secondPressPos))
                    {
                        firstPressPos = Vector2.zero;
                        NativeInterface.Vibrate();
                        onSwipeDetected?.Invoke();
                    }
                }
            }
        }
    }

    private bool isSwipeInTheRightDirection(Vector2 firstPressPos, Vector2 secondPressPos)
    {
        switch (direction)
        {
            case Direction.Up:
                return Math.Abs(firstPressPos.x - secondPressPos.x) < maxSwipeDeviation && firstPressPos.y < secondPressPos.y;

            case Direction.Right:
                return Math.Abs(firstPressPos.y - secondPressPos.y) < maxSwipeDeviation && firstPressPos.x < secondPressPos.x;

            case Direction.Down:
                return Math.Abs(firstPressPos.x - secondPressPos.x) < maxSwipeDeviation && firstPressPos.y > secondPressPos.y;

            case Direction.Left:
                return Math.Abs(firstPressPos.y - secondPressPos.y) < maxSwipeDeviation && firstPressPos.x > secondPressPos.x;
        }

        return false;
    }

    private void checkTouchIsInsideRect()
    {
        TouchStartedFromThisArea = false;

        if (InputHelper.GetTouches().Count > 0)
        {
            Touch touch = InputHelper.GetTouches()[0];

            if (RectTransformUtility.RectangleContainsScreenPoint(RectTransform, touch.position))
            {
                TouchStartedFromThisArea = true;
            }

        }
    }
}
