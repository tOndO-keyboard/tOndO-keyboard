using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Events;

namespace Tweens
{
    public abstract class Tween : MonoBehaviour
    {
        public static Tween current;

        public static void Play(Tween tweener, bool forward = true)
        {
            if (tweener == null)
            {
                return;
            }

            tweener.Play(forward);
        }

        public static void Play<TweenerType>(List<TweenerType> tweeners, bool forward = true) where TweenerType : Tween
        {
            if (tweeners == null)
            {
                return;
            }

            foreach (TweenerType tweener in tweeners)
            {
                Play(tweener, forward);
            }
        }

        public static void ResetToBeginning(List<Tween> tweeners)
        {
            if (tweeners == null)
            {
                return;
            }

            foreach (Tween tweener in tweeners)
            {
                if (tweener != null)
                {
                    tweener.ResetToBeginning();
                }
            }
        }

        public enum Method
        {
            Linear,
            EaseIn,
            EaseOut,
            EaseInOut,
            BounceIn,
            BounceOut,
        }

        public enum Style
        {
            Once,
            Loop,
            PingPong,
            PingPongOnce
        }

        public enum Direction
        {
            Reverse = -1,
            Toggle = 0,
            Forward = 1,
        }

        /// <summary>
        /// Tweening method used.
        /// </summary>
        /// 

        public Method method = Method.Linear;

        /// <summary>
        /// Does it play once? Does it loop?
        /// </summary>

        public Style style = Style.Once;

        /// <summary>
        /// Optional curve to apply to the tween's time factor value.
        /// </summary>

        public AnimationCurve animationCurve = new AnimationCurve(new Keyframe(0f, 0f, 0f, 1f), new Keyframe(1f, 1f, 1f, 0f));

        /// <summary>
        /// Whether the tween will ignore the timescale, making it work while the game is paused.
        /// </summary>

        public bool ignoreTimeScale = true;

        /// <summary>
        /// How long will the tweener wait before starting the tween?
        /// </summary>

        public float delay = 0f;

        /// <summary>
        /// How long is the duration of the tween?
        /// </summary>

        public float duration = 1f;

        /// <summary>
        /// Whether the tweener will use steeper curves for ease in / out style interpolation.
        /// </summary>

        public bool steeperCurves = false;

        /// <summary>
        /// Used by buttons and tween sequences. Group of '0' means not in a sequence.
        /// </summary>

        public int tweenGroup = 0;

        /// <summary>
        /// UnityEvent called when the animation finishes.
        /// </summary>

        public UnityEvent onFinished = new UnityEvent();

        private bool started = false;
        private float startTime = 0f;
        private float mDuration = 0f;
        private float amountPerDelta = 1000f;
        private float factor = 0f;

        /// <summary>
        /// Amount advanced per delta time.
        /// </summary>

        public float AmountPerDelta
        {
            get
            {
                if (mDuration != duration)
                {
                    mDuration = duration;
                    amountPerDelta = Mathf.Abs((duration > 0f) ? 1f / duration : 1000f) * Mathf.Sign(amountPerDelta);
                }

                return amountPerDelta;
            }
        }

        /// <summary>
        /// Tween factor, 0-1 range.
        /// </summary>

        public float TweenFactor
        {
            get { return factor; }
            set { factor = Mathf.Clamp01(value); }
        }

        /// <summary>
        /// Direction that the tween is currently playing in.
        /// </summary>

        public Direction PlayingDirection
        {
            get { return AmountPerDelta < 0f ? Direction.Reverse : Direction.Forward; }
        }

        /// <summary>
        /// This function is called by Unity when you add a component. Automatically set the starting values for convenience.
        /// </summary>

        private void Reset()
        {
            if (!started)
            {
                SetStartToCurrentValue();
                SetEndToCurrentValue();
            }
        }

        /// <summary>
        /// Update as soon as it's started so that there is no delay.
        /// </summary>

        protected virtual void Start()
        {
            Update();
        }

        /// <summary>
        /// Update the tweening factor and call the virtual update function.
        /// </summary>

        private void Update()
        {
            float delta = Time.deltaTime;
            float time = Time.time;

            if (!started)
            {
                started = true;
                startTime = time + delay;
            }

            if (time < startTime) return;

            // Advance the sampling factor
            factor += AmountPerDelta * delta;

            // Loop style simply resets the play factor after it exceeds 1.
            if (style == Style.Loop)
            {
                if (factor > 1f)
                {
                    factor -= Mathf.Floor(factor);
                }
            }
            else if (style == Style.PingPong)
            {
                // Ping-pong style reverses the direction
                if (factor > 1f)
                {
                    factor = 1f - (factor - Mathf.Floor(factor));
                    amountPerDelta = -amountPerDelta;
                }
                else if (factor < 0f)
                {
                    factor = -factor;
                    factor -= Mathf.Floor(factor);
                    amountPerDelta = -amountPerDelta;
                }
            }
            else if (style == Style.PingPongOnce)
            {
                if (factor > 1f)
                {
                    factor = 1f - (factor - Mathf.Floor(factor));
                    amountPerDelta = -amountPerDelta;
                }
            }

            // If the factor goes out of range and this is a one-time tweening operation, disable the script
            if (((style == Style.Once) && (duration == 0f || factor > 1f || factor < 0f)) || ((style == Style.PingPongOnce) && factor < 0f))
            {
                factor = Mathf.Clamp01(factor);
                Sample(factor, true);
                enabled = false;

                if (current == null)
                {
                    Tween before = current;
                    current = this;

                    if (onFinished != null)
                    {
                        onFinished.Invoke();
                        onFinished.RemoveAllListeners();
                    }

                    current = before;
                }
            }
            else Sample(factor, false);
        }

        /// <summary>
        /// Convenience function -- set a new OnFinished event delegate (here for to be consistent with RemoveOnFinished).
        /// </summary>

        public void SetOnFinished(UnityAction del)
        {
            onFinished.RemoveAllListeners();
            onFinished.AddListener(del);
        }

        /// <summary>
        /// Convenience function -- add a new OnFinished event delegate (here for to be consistent with RemoveOnFinished).
        /// </summary>

        public void AddOnFinished(UnityAction del)
        {
            onFinished.AddListener(del);
        }

        /// <summary>
        /// Remove an OnFinished delegate. Will work even while iterating through the list when the tweener has finished its operation.
        /// </summary>

        public void RemoveOnFinished(UnityAction del)
        {
            onFinished.RemoveListener(del);
        }

        /// <summary>
        /// Mark as not started when finished to enable delay on next play.
        /// </summary>

        private void OnDisable()
        {
            started = false;
        }

        /// <summary>
        /// Sample the tween at the specified factor.
        /// </summary>

        public void Sample(float factor, bool isFinished)
        {
            // Calculate the sampling value
            float val = Mathf.Clamp01(factor);

            if (method == Method.EaseIn)
            {
                val = 1f - Mathf.Sin(0.5f * Mathf.PI * (1f - val));
                if (steeperCurves) val *= val;
            }
            else if (method == Method.EaseOut)
            {
                val = Mathf.Sin(0.5f * Mathf.PI * val);

                if (steeperCurves)
                {
                    val = 1f - val;
                    val = 1f - val * val;
                }
            }
            else if (method == Method.EaseInOut)
            {
                const float pi2 = Mathf.PI * 2f;
                val = val - Mathf.Sin(val * pi2) / pi2;

                if (steeperCurves)
                {
                    val = val * 2f - 1f;
                    float sign = Mathf.Sign(val);
                    val = 1f - Mathf.Abs(val);
                    val = 1f - val * val;
                    val = sign * val * 0.5f + 0.5f;
                }
            }
            else if (method == Method.BounceIn)
            {
                val = BounceLogic(val);
            }
            else if (method == Method.BounceOut)
            {
                val = 1f - BounceLogic(1f - val);
            }

            // Call the virtual update
            OnUpdate((animationCurve != null) ? animationCurve.Evaluate(val) : val, isFinished);
        }

        /// <summary>
        /// Main Bounce logic to simplify the Sample function
        /// </summary>

        private float BounceLogic(float val)
        {
            if (val < 0.363636f) // 0.363636 = (1/ 2.75)
            {
                val = 7.5685f * val * val;
            }
            else if (val < 0.727272f) // 0.727272 = (2 / 2.75)
            {
                val = 7.5625f * (val -= 0.545454f) * val + 0.75f; // 0.545454f = (1.5 / 2.75) 
            }
            else if (val < 0.909090f) // 0.909090 = (2.5 / 2.75) 
            {
                val = 7.5625f * (val -= 0.818181f) * val + 0.9375f; // 0.818181 = (2.25 / 2.75) 
            }
            else
            {
                val = 7.5625f * (val -= 0.9545454f) * val + 0.984375f; // 0.9545454 = (2.625 / 2.75) 
            }
            return val;
        }

        /// <summary>
        /// Play the tween forward.
        /// </summary>

        public void PlayForward(bool resetBeforePlaying = false) { Play(true, resetBeforePlaying); }

        /// <summary>
        /// Play the tween in reverse.
        /// </summary>

        public void PlayReverse(bool resetBeforePlaying = false) { Play(false, resetBeforePlaying); }

        /// <summary>
        /// Manually activate the tweening process, reversing it if necessary.
        /// </summary>

        public void Play(bool forward, bool resetBeforePlaying = false)
        {
            amountPerDelta = Mathf.Abs(AmountPerDelta);

            if (!forward)
            {
                amountPerDelta = -amountPerDelta;
                factor = 1f;
            }

            if (resetBeforePlaying)
            {
                ResetToBeginning();
            }

            enabled = true;

            Update();
        }

        public void Stop()
        {
            ResetToBeginning();
            enabled = false;
        }

        /// <summary>
        /// Manually reset the tweener's state to the beginning.
        /// If the tween is playing forward, this means the tween's start.
        /// If the tween is playing in reverse, this means the tween's end.
        /// </summary>

        public virtual void ResetToBeginning()
        {
            started = false;
            factor = (AmountPerDelta < 0f) ? 1f : 0f;
            Sample(factor, false);
        }

        /// <summary>
        /// Manually start the tweening process, reversing its direction.
        /// </summary>

        public void Toggle()
        {
            if (factor > 0f)
            {
                amountPerDelta = -AmountPerDelta;
            }
            else
            {
                amountPerDelta = Mathf.Abs(AmountPerDelta);
            }
            enabled = true;
        }

        /// <summary>
        /// Actual tweening logic should go here.
        /// </summary>

        protected abstract void OnUpdate(float factor, bool isFinished);

        /// <summary>
        /// Starts the tweening operation.
        /// </summary>

        public static T Begin<T>(GameObject go, float duration) where T : Tween
        {
            T comp = go.GetComponent<T>();

            // Find the tween with an unset group ID (group ID of 0).
            if (comp != null && comp.tweenGroup != 0)
            {
                comp = null;
                T[] comps = go.GetComponents<T>();

                for (int i = 0, imax = comps.Length; i < imax; ++i)
                {
                    comp = comps[i];
                    if (comp != null && comp.tweenGroup == 0) break;
                    comp = null;
                }
            }

            if (comp == null)
            {
                comp = go.AddComponent<T>();

                if (comp == null)
                {
                    Debug.LogError("Unable to add " + typeof(T) + " to " + go.name, go);
                    return null;
                }
            }

            comp.started = false;
            comp.duration = duration;
            comp.factor = 0f;
            comp.amountPerDelta = Mathf.Abs(comp.AmountPerDelta);
            comp.style = Style.Once;
            comp.animationCurve = new AnimationCurve(new Keyframe(0f, 0f, 0f, 1f), new Keyframe(1f, 1f, 1f, 0f));
            comp.enabled = true;

            return comp;
        }

        /// <summary>
        /// Set the 'from' value to the current one.
        /// </summary>

        public virtual void SetStartToCurrentValue() { }

        /// <summary>
        /// Set the 'to' value to the current one.
        /// </summary>

        public virtual void SetEndToCurrentValue() { }
    }
}