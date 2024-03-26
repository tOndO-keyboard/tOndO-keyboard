using UnityEngine;
using UnityEngine.UI;

namespace Tweens
{
    /// <summary>
    /// Tween the fillAmount of a filled Image.
    /// </summary>

    [AddComponentMenu("Tween/Tween Fill Amount")]
    public class TweenFillAmount : Tween
    {
        [Range(0f, 1f)]
        public float from = 1f;

        [Range(0f, 1f)]
        public float to = 1f;

        private bool cached = false;

        private Image image;

        private void Cache()
        {
            cached = true;

            image = GetComponent<Image>();
        }

        /// <summary>
        /// Tween's current value.
        /// </summary>

        public float value
        {
            get
            {
                if (!cached)
                {
                    Cache();
                }

                if (image != null)
                {
                    return image.fillAmount;
                }
                else
                {
                    return 0;
                }
            }
            set
            {
                if (!cached)
                {
                    Cache();
                }

                if (image != null)
                {
                    image.fillAmount = value;
                }
            }
        }

        /// <summary>
        /// Tween the value.
        /// </summary>

        protected override void OnUpdate(float factor, bool isFinished)
        {
            value = Mathf.Lerp(from, to, factor);
        }

        /// <summary>
        /// Start the tweening operation.
        /// </summary>

        public static TweenFillAmount Begin(GameObject go, float duration, float fillAmount)
        {
            TweenFillAmount comp = Tween.Begin<TweenFillAmount>(go, duration);
            comp.from = comp.value;
            comp.to = fillAmount;

            if (duration <= 0f)
            {
                comp.Sample(1f, true);
                comp.enabled = false;
            }
            return comp;
        }

        public override void SetStartToCurrentValue()
        {
            from = value;
        }

        public override void SetEndToCurrentValue()
        {
            to = value;
        }
    }

}