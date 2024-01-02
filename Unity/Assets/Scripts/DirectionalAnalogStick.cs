using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

public class DirectionalAnalogStick : AnalogStick
{
    [SerializeField]
    private float maxSubmitDelay = 0.25f;

    [SerializeField]
    private float minSubmitDelay = 0.01f;

    private float lastSubmitTime = 0;

    private float currentSubmitDelay;
    private int movementEntity = 1;
    private CardinalDirection previousSelectedDirection;
    private RectTransform canvasRectTransform;

    protected override void Awake()
    {
        base.Awake();
        currentSubmitDelay = maxSubmitDelay;
        canvasRectTransform = canvasRectTransform = GetComponentInParent<Canvas>().GetComponent<RectTransform>();
    }

    private void Update()
    {
#if UNITY_EDITOR
        if(InputHelper.GetTouches().Count > 0 && Selected)
        {
            Touch touch = InputHelper.GetTouches()[0];
#else
        if(InputHelper.GetTouches().Count > 0 && InputHelper.GetTouches().Count <= touchIndex)
        {
            touchIndex = Input.touches.Length - 1;
        }

        if (Input.touches.Length > 0 && Input.touches.Length > touchIndex && Selected)
        {
            Touch touch = Input.GetTouch(touchIndex);
#endif
            if (touch.phase == TouchPhase.Began)
            {
                currentSubmitDelay = maxSubmitDelay;
                lastSubmitTime = 0;
                firstPressPos = new Vector2(touch.position.x, touch.position.y);
            }
            else if (touch.phase == TouchPhase.Moved || touch.phase == TouchPhase.Stationary || touch.phase == TouchPhase.Canceled || touch.phase == TouchPhase.Ended)
            {
                secondPressPos = new Vector2(touch.position.x, touch.position.y);

                float angle = 0;

                int cloveNumber = validCardinalDirection.Length;
                int cloveAmplitude = 360 / cloveNumber;

                angle = Mathf.Atan2(secondPressPos.y - firstPressPos.y, secondPressPos.x - firstPressPos.x) * 180 / Mathf.PI;

                if (angle < 0) angle = 360 + angle;

                float distance = Vector2.Distance(secondPressPos, firstPressPos);
                
                angle = MathUtils.GetClosestMultiple(angle, cloveAmplitude);
                if (angle == 360) angle = 0;

                float distanceJ = distance * 0.5f;
                distanceJ = distanceJ > rectTransform.rect.size.x * 0.09f ? rectTransform.rect.size.x * 0.09f : distanceJ;

                float angleRad = angle * Mathf.Deg2Rad;

                float deltaX = distanceJ * Mathf.Cos(angleRad);
                float deltaY = distanceJ * Mathf.Cos(90 * Mathf.Deg2Rad - angleRad);

                StopLerpAnalogStick();
                contentTransform.localPosition = new Vector3(initialPosition.x + deltaX, initialPosition.y + deltaY, initialPosition.z);

                selectedDirection = CardinalDirection.Null;

                if (distance > rectTransform.rect.size.x * 0.025f)
                {
                    float firstDelta = -(cloveAmplitude / 2);

                    for (int i = 0; i < cloveNumber; i++)
                    {
                        if (angle >= firstDelta + cloveAmplitude * i &&
                            angle < firstDelta + cloveAmplitude * (i + 1))
                        {
                            selectedDirection = validCardinalDirection[i];
                        }
                    }
                }

                if(!firstDirectionSelected && previousSelectedDirection != selectedDirection)
                {
                    nativeInterface.Vibrate();
                }
                previousSelectedDirection = selectedDirection;
                if (selectedDirection != CardinalDirection.Center && selectedDirection != CardinalDirection.Null)
                {
                    firstDirectionSelected = false;
                }

                float maxSpeedDistance = canvasRectTransform.rect.width * 0.5f;

                currentSubmitDelay = Mathf.Lerp(maxSubmitDelay, minSubmitDelay, distance / maxSpeedDistance);

                SubmitSelected();
            }
        }
        else if(InputHelper.GetTouches().Count == 0)
        {
            Deselect();
        }

        LerpAnalogStickToInitialLocation();
    }

    protected override void SubmitSelected()
    {
        if (selectedDirection == CardinalDirection.Null) return;

        float timeOffset = Time.time - lastSubmitTime;
        if (Time.time - lastSubmitTime > currentSubmitDelay)
        {
            lastSubmitTime = Time.time;

            switch(selectedDirection)
            {
                case CardinalDirection.West:
                case CardinalDirection.SouthWest:
                case CardinalDirection.NorthWest:
                    nativeInterface.MoveCursorLeft(movementEntity);
                    break;
                case CardinalDirection.East:
                case CardinalDirection.SouthEast:
                case CardinalDirection.NorthEast:
                    nativeInterface.MoveCursorRight(movementEntity);
                    break;
                case CardinalDirection.North:
                    nativeInterface.MoveCursorUp(movementEntity);
                    break;
                case CardinalDirection.South:
                    nativeInterface.MoveCursorDown(movementEntity);
                    break;
            }

            selectedDirection = CardinalDirection.Null;
        }
    }
}