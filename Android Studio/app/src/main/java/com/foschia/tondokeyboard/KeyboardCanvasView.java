package com.foschia.tondokeyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Draws and handles the touch input of the "main" keyboard: the top action bar and the six tondos
 * (analog sticks) with shift / symbols / backspace / done buttons.
 * <p>
 * Everything is laid out in a virtual coordinate system 1080 units wide (or wider in landscape) and
 * {@value #BOTTOM_HEIGHT} + bar height units tall, that is the same reference resolution the original
 * canvas used, and is scaled to the real view size.
 */
public class KeyboardCanvasView extends View
{
	public static final float BOTTOM_HEIGHT = 583f;
	public static final float BAR_HEIGHT = 137f;
	public static final float HIDDEN_BAR_HEIGHT = 10f;
	public static final float REF_WIDTH = 1080f;
	public static final float LANDSCAPE_MIN_PROPORTION = 2.1f;

	private static final float TONDO_RADIUS = 140f;
	private static final float GLYPH_RADIUS = 100f;
	private static final float BUTTON_RADIUS = 58f;
	private static final long LONG_PRESS_MS = 250;

	public enum Bar {MAIN, CTRL, SKIN_TONES}

	public enum ShiftState {NOT_PRESSED, PRESSED, DOUBLE_PRESSED}

	/** What the keyboard needs from its container. */
	public interface Host
	{
		void onViewRequested(KeyboardRootView.Content content);

		KeyboardRootView.Content getContent();

		boolean isEmojiMode();

		void onTopBarHiddenChanged(boolean hidden);

		boolean isPro();
	}

	private final Host host;
	private final KbSettings settings = new KbSettings();
	private Palette palette = Palette.LIGHT;
	private CharacterMap charMap;
	private DiacriticsMap diacritics;

	// ---- state
	private Bar bar = Bar.MAIN;
	private CharacterMap.Layout layout = CharacterMap.Layout.MAIN;
	private ShiftState shift = ShiftState.PRESSED;
	private boolean setPressedAfterNextCommit = false;
	private int editorInputType = KeyboardActionListener.TEXT_INPUT;
	private String precedingCharacter = "";
	private boolean lastCommitWasEmoji = false;
	private int doneFlag = KeyboardActionListener.DONE_ACTION_FLAG_NEWLINE;
	private long lastShiftTime = 0;
	private boolean landscape = false;
	private boolean platformLandscape = false;

	// ---- geometry
	private float scale = 1f;
	private float virtualWidth = REF_WIDTH;
	private float barTop, contentTop;

	// ---- objects
	private final List<Tondo> tondos = new ArrayList<>();
	private final List<Btn> mainButtons = new ArrayList<>();
	private final List<Btn> barButtons = new ArrayList<>();
	private final List<Btn> ctrlButtons = new ArrayList<>();
	private final List<Btn> skinButtons = new ArrayList<>();
	private final List<Btn> permanentButtons = new ArrayList<>();
	private CursorStick cursorStick, cursorStick2;
	private Btn shiftBtn, shiftBtn2, symbolsBtn, symbolsBtn2, symbols2Btn, symbols2Btn2, backspaceBtn, backspaceBtn2, doneBtn, doneBtn2, hideBtn;
	private Btn skinNoneBtn, emojiBackBtn;

	private final SparseArray<Elem> pointerTargets = new SparseArray<>();
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Map<Integer, Bitmap> bitmaps = new java.util.HashMap<>();
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Runnable ticker = new Runnable()
	{
		@Override
		public void run()
		{
			boolean active = false;
			for (int i = 0; i < pointerTargets.size(); i++)
			{
				Elem e = pointerTargets.valueAt(i);
				if (e != null)
				{
					e.tick(SystemClock.uptimeMillis());
					active = true;
				}
			}
			if (active)
			{
				invalidate();
				handler.postDelayed(this, 20);
			}
		}
	};

	private final NativeInterface.ImeEventListener imeListener = new NativeInterface.ImeEventListener()
	{
		@Override
		public void onStartInputView(String preceding)
		{
			reloadSettings();
			precedingCharacter = preceding == null ? "" : preceding;
			bar = Bar.MAIN;
			layout = CharacterMap.Layout.MAIN;
			doneFlag = NativeInterface.getInstance().GetDoneActionFlag();
			if (!settings.disableAutomaticCapitalization)
				setShift(precedingCharacter.isEmpty() ? ShiftState.PRESSED : ShiftState.NOT_PRESSED);
			invalidateLayout();
		}

		@Override
		public void onSetInputType(int type)
		{
			editorInputType = type;
			boolean numerical = (type & KeyboardActionListener.NUMERICAL_INPUT) > 0;
			boolean password = (type & KeyboardActionListener.PASSWORD_INPUT) > 0;
			if (!settings.disableAutomaticCapitalization && password) setShift(ShiftState.NOT_PRESSED);
			layout = numerical ? CharacterMap.Layout.SYMBOLS1 : CharacterMap.Layout.MAIN;
			invalidate();
		}

		@Override
		public void onCursorPositionChanged(String preceding)
		{
			precedingCharacter = preceding == null ? "" : preceding;
			if (!settings.disableAutomaticCapitalization && shift == ShiftState.NOT_PRESSED
					&& precedingCharacter.isEmpty() && (editorInputType & KeyboardActionListener.PASSWORD_INPUT) == 0)
				setShift(ShiftState.PRESSED);
			invalidate();
		}

		@Override
		public void onConfigurationChanged()
		{
			requestLayout();
			invalidate();
		}
	};

	public KeyboardCanvasView(Context context, Host host)
	{
		super(context);
		this.host = host;
		setLayerType(LAYER_TYPE_HARDWARE, null);
		textPaint.setTextAlign(Paint.Align.CENTER);
		reloadSettings();
		buildObjects();
		// Registered for the whole life of the view: onStartInputView may arrive before the view is attached.
		NativeInterface.getInstance().addImeListener(imeListener);
	}

	// ------------------------------------------------------------------------------------------
	// Public API
	// ------------------------------------------------------------------------------------------

	public KbSettings getSettings()
	{
		return settings;
	}

	public Palette getPalette()
	{
		return palette;
	}

	public void setPalette(Palette p)
	{
		palette = p;
		invalidate();
	}

	public void setBar(Bar b)
	{
		bar = b;
		invalidate();
	}

	public Bar getBar()
	{
		return bar;
	}

	public float getBarHeightUnits()
	{
		return settings.topBarHidden ? HIDDEN_BAR_HEIGHT : BAR_HEIGHT;
	}

	public void setPlatformLandscape(boolean l)
	{
		platformLandscape = l;
		requestLayout();
	}

	public boolean isLandscapeLayout()
	{
		return landscape;
	}

	public void invalidateLayout()
	{
		requestLayout();
		invalidate();
	}

	public boolean isLastCommitEmoji()
	{
		return lastCommitWasEmoji;
	}

	public String getPrecedingCharacter()
	{
		return precedingCharacter;
	}

	public float getScale()
	{
		return scale;
	}

	/** Called by the container when a commit happened outside the tondos (emoji, macros, speech...). */
	public void onExternalCommit(boolean emoji)
	{
		lastCommitWasEmoji = emoji;
		precedingCharacter = NativeInterface.getInstance().GetPrecedingCharacter(1);
		invalidate();
	}

	public void onDoneCommitted()
	{
		if (settings.disableAutomaticCapitalization) return;
		if (shift == ShiftState.NOT_PRESSED && doneFlag == KeyboardActionListener.DONE_ACTION_FLAG_NEWLINE)
			setShift(ShiftState.PRESSED);
	}

	public void reloadSettings()
	{
		settings.load(getContext());
		charMap = new CharacterMap(settings.characterCustomization);
		diacritics = new DiacriticsMap(settings.diacriticsCustomization);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		handler.removeCallbacks(ticker);
		super.onDetachedFromWindow();
	}

	// ------------------------------------------------------------------------------------------
	// Layout
	// ------------------------------------------------------------------------------------------

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		computeGeometry(w, h);
	}

	private void computeGeometry(int w, int h)
	{
		if (w <= 0 || h <= 0) return;
		float totalH = BOTTOM_HEIGHT + getBarHeightUnits();
		scale = Math.min(h / totalH, w / REF_WIDTH);
		virtualWidth = w / scale;
		landscape = platformLandscape && virtualWidth / totalH >= LANDSCAPE_MIN_PROPORTION;
		float barH = getBarHeightUnits();
		if (settings.topBarWentBottom)
		{
			contentTop = 0;
			barTop = BOTTOM_HEIGHT;
		}
		else
		{
			barTop = 0;
			contentTop = barH;
		}
		positionObjects();
	}

	/** Content rect (virtual units) that overlays use. */
	public float getContentTopPx()
	{
		return contentTop * scale;
	}

	public float getContentHeightPx()
	{
		return BOTTOM_HEIGHT * scale;
	}

	// ------------------------------------------------------------------------------------------
	// Objects
	// ------------------------------------------------------------------------------------------

	private abstract class Elem
	{
		float cx, cy;     // virtual position of the centre
		boolean visible = true;

		abstract boolean hit(float x, float y);

		abstract void draw(Canvas c);

		void down(float x, float y, long now) {}

		void move(float x, float y, long now) {}

		void up(float x, float y, long now) {}

		void cancel() {}

		void tick(long now) {}
	}

	/** A round button with an icon. */
	private class Btn extends Elem
	{
		int icon;
		int iconColor, bgColor;
		Runnable onTrigger, onLongPress;
		boolean triggerOnDown;
		boolean pressed;
		boolean flipV;
		long downTime;
		boolean longDone;
		float radius = BUTTON_RADIUS;
		float w = 116, h = 116;
		boolean showBackground = true;
		boolean shadow = true;

		Btn(int icon, Runnable onTrigger)
		{
			this.icon = icon;
			this.onTrigger = onTrigger;
		}

		@Override
		boolean hit(float x, float y)
		{
			return Math.abs(x - cx) <= w / 2 && Math.abs(y - cy) <= h / 2;
		}

		@Override
		void draw(Canvas c)
		{
			if (showBackground)
			{
				if (shadow && !pressed)
				{
					paint.setColor(palette.shadow);
					c.drawCircle(cx, cy + 8, radius, paint);
				}
				paint.setColor(bgColor);
				c.drawCircle(cx, cy + (pressed ? 3 : 0), radius, paint);
			}
			drawIcon(c, icon, cx, cy + (pressed ? 3 : 0), 100, iconColor, flipV);
		}

		@Override
		void down(float x, float y, long now)
		{
			pressed = true;
			downTime = now;
			longDone = false;
			vibrate();
			if (triggerOnDown && onTrigger != null) onTrigger.run();
		}

		@Override
		void tick(long now)
		{
			if (onLongPress != null && !longDone && pressed && now - downTime > 500)
			{
				longDone = true;
				vibrate();
				onLongPress.run();
			}
		}

		@Override
		void up(float x, float y, long now)
		{
			pressed = false;
			if (triggerOnDown || longDone) return;
			vibrate();
			if (onTrigger != null) onTrigger.run();
		}

		@Override
		void cancel()
		{
			pressed = false;
		}
	}

	/** The long glyph free rounded background behind a pair of permanent buttons. */
	private class Capsule extends Elem
	{
		Btn a, b;
		int color;

		@Override
		boolean hit(float x, float y)
		{
			return false;
		}

		@Override
		void draw(Canvas c)
		{
			float top = Math.min(a.cy, b.cy) - BUTTON_RADIUS;
			float bottom = Math.max(a.cy, b.cy) + BUTTON_RADIUS;
			RectF r = new RectF(a.cx - BUTTON_RADIUS, top, a.cx + BUTTON_RADIUS, bottom);
			paint.setColor(palette.shadow);
			RectF rs = new RectF(r);
			rs.offset(0, 8);
			c.drawRoundRect(rs, BUTTON_RADIUS, BUTTON_RADIUS, paint);
			paint.setColor(color);
			c.drawRoundRect(r, BUTTON_RADIUS, BUTTON_RADIUS, paint);
		}
	}

	private final List<Capsule> capsules = new ArrayList<>();

	/** The tondo: an analog stick selecting one of 7 glyphs. */
	private class Tondo extends Elem
	{
		final CharacterMap.Stick position;
		final boolean landscapeOnly;
		final boolean mirrorsCentre; // landscape duplicate

		Dir selected = null;
		boolean tracking;
		boolean firstDirectionSelected;
		float startX, startY;
		float needleAngle, needleLength, doubleLength;
		float jx, jy; // joystick offset
		float jTargetX, jTargetY;
		long lastDirChange;
		boolean longPressed;
		boolean replaced;
		String[] usedAlternates;
		boolean usedLeft;
		final Map<Dir, String> tempLabels = new EnumMap<>(Dir.class);

		Tondo(CharacterMap.Stick position, boolean landscapeOnly)
		{
			this.position = position;
			this.landscapeOnly = landscapeOnly;
			this.mirrorsCentre = landscapeOnly;
		}

		boolean isLeftDiacriticizer(Dir d)
		{
			return position == CharacterMap.Stick.TOP_LEFT && d == Dir.NORTH_WEST;
		}

		boolean isDiacriticizer(Dir d)
		{
			return isLeftDiacriticizer(d) || (position == CharacterMap.Stick.TOP_RIGHT && d == Dir.NORTH_EAST);
		}

		@Override
		boolean hit(float x, float y)
		{
			float dx = x - cx, dy = y - cy;
			return dx * dx + dy * dy <= TONDO_RADIUS * TONDO_RADIUS;
		}

		String glyph(Dir d)
		{
			if (isDiacriticizer(d))
			{
				String last = diacritics.lastSelected(precedingCharacter, isLeftDiacriticizer(d));
				return last == null || lastCommitWasEmoji ? "" : last;
			}
			String temp = tempLabels.get(d);
			if (temp != null) return temp;
			String g = charMap.get(layout, position, d);
			if (layout == CharacterMap.Layout.MAIN && g.length() > 0 && Character.isLetter(g.codePointAt(0)) && g.codePointCount(0, g.length()) == 1)
				g = shift != ShiftState.NOT_PRESSED ? g.toUpperCase() : g.toLowerCase();
			return g;
		}

		/** Glyph as shown on screen (space is drawn as a visible symbol). */
		String displayGlyph(Dir d)
		{
			String g = glyph(d);
			return g.equals(" ") ? "␣" : g;
		}

		@Override
		void down(float x, float y, long now)
		{
			tracking = true;
			startX = x;
			startY = y;
			vibrate();
			firstDirectionSelected = true;
			selected = Dir.CENTER;
			lastDirChange = now;
			longPressed = false;
		}

		@Override
		void move(float x, float y, long now)
		{
			if (!tracking) return;
			float dx = x - startX, dy = y - startY;
			float distance = (float) Math.hypot(dx, dy);
			float angle = StickMath.angle(startX, startY, x, y, settings.reverse);
			angle = StickMath.stick(angle, Dir.TONDO_SECTORS.length, settings.sticky, settings.verySticky);
			float ang = (float) Math.toRadians(angle);
			float width = TONDO_RADIUS * 2;
			if (settings.inputType == KbSettings.INPUT_JOYSTICK)
			{
				float d = Math.min(distance * 0.5f, width * 0.06f);
				jx = d * (float) Math.cos(ang);
				jy = -d * (float) Math.sin(ang);
			}
			else if (settings.inputType == KbSettings.INPUT_DOUBLE_NEEDLE)
			{
				needleAngle = angle;
				doubleLength = Math.min(distance * 2, width);
			}
			else
			{
				needleAngle = angle;
				needleLength = Math.min(distance, width * 0.5f);
			}

			Dir newDir = StickMath.select(angle, distance, width, settings.externalGlyphSelectionDistanceFactor, Dir.TONDO_SECTORS);
			if (selected != null && !firstDirectionSelected && newDir != selected) vibrate();
			if (selected != null && selected != Dir.CENTER) firstDirectionSelected = false;
			if (newDir != selected)
			{
				lastDirChange = now;
				longPressed = false;
			}
			selected = newDir;
		}

		@Override
		void tick(long now)
		{
			if (!tracking || selected == null) return;
			if (!longPressed && now - lastDirChange > LONG_PRESS_MS)
			{
				longPressed = true;
				firstDirectionSelected = false;
				replaceWithAlternatives();
			}
		}

		private void replaceWithAlternatives()
		{
			if (replaced || selected == null || !isDiacriticizer(selected)) return;
			boolean left = isLeftDiacriticizer(selected);
			String[] alternates = diacritics.alternates(precedingCharacter, left);
			if (alternates == null || alternates.length == 0 || lastCommitWasEmoji) return;
			List<Dir> sorted = sortedDirs(left);
			int index = 0;
			for (Dir d : sorted)
			{
				if (isDiacriticizer(d)) continue;
				tempLabels.put(d, index < alternates.length ? alternates[index] : "");
				index++;
			}
			usedAlternates = alternates;
			usedLeft = left;
			replaced = true;
		}

		/** Directions ordered circularly from north, clockwise for the left diacriticizer, centre last. */
		private List<Dir> sortedDirs(boolean clockwise)
		{
			List<Dir> dirs = new ArrayList<>();
			for (Dir d : Dir.values()) if (d != Dir.NORTH && d != Dir.SOUTH) dirs.add(d);
			final int starter = Dir.NORTH.ordinal();
			final int last = Dir.SOUTH_EAST.ordinal();
			java.util.Collections.sort(dirs, (a, b) -> {
				if (a == b) return 0;
				if (a == Dir.CENTER) return 1;
				if (b == Dir.CENTER) return -1;
				int ia = a.ordinal(), ib = b.ordinal();
				if (!clockwise)
				{
					ia -= starter;
					if (ia < 0) ia += last + 1;
					ib -= starter;
					if (ib < 0) ib += last + 1;
					return Integer.compare(ia, ib);
				}
				ia += last - starter;
				if (ia > last) ia -= last + 1;
				ib += last - starter;
				if (ib > last) ib -= last + 1;
				return Integer.compare(ib, ia);
			});
			return dirs;
		}

		@Override
		void up(float x, float y, long now)
		{
			if (tracking) submit();
			reset();
		}

		@Override
		void cancel()
		{
			reset();
		}

		private void reset()
		{
			tracking = false;
			selected = null;
			needleLength = 0;
			doubleLength = 0;
			jx = jy = 0;
			tempLabels.clear();
			replaced = false;
			longPressed = false;
		}

		private void submit()
		{
			if (selected == null) return;
			boolean diacriticizer = isDiacriticizer(selected);
			String text;
			if (diacriticizer)
			{
				text = diacritics.lastSelected(precedingCharacter, isLeftDiacriticizer(selected));
				if (text == null || lastCommitWasEmoji) text = "";
			}
			else
			{
				text = glyph(selected);
			}

			if (replaced && usedAlternates != null)
			{
				for (int i = 0; i < usedAlternates.length; i++)
				{
					if (usedAlternates[i].equals(text))
					{
						if (!lastCommitWasEmoji) diacritics.setLastSelectedIndex(precedingCharacter, i, usedLeft);
						break;
					}
				}
			}

			if (!text.isEmpty())
			{
				NativeInterface ni = NativeInterface.getInstance();
				onGlyphCommitting(precedingCharacter, text);
				if (diacriticizer || replaced) ni.ReplaceLastCharacterWith(text);
				else ni.CommitString(text);
				lastCommitWasEmoji = false;
				precedingCharacter = ni.GetPrecedingCharacter(1);
				onGlyphCommitted(text);
			}
		}

		@Override
		void draw(Canvas c)
		{
			c.save();
			c.translate(cx, cy);
			// Shadow
			paint.setColor(palette.shadow);
			c.drawCircle(0, 12, TONDO_RADIUS, paint);
			c.translate(jx, jy);
			paint.setColor(palette.tondosExterior);
			c.drawCircle(0, 0, TONDO_RADIUS, paint);
			// centre key
			paint.setColor(palette.shadow);
			c.drawCircle(0, 8, 58, paint);
			paint.setColor(palette.tondosInterior);
			c.drawCircle(0, 0, 58, paint);

			boolean popup = settings.showPopupPreview;
			boolean stickyNeedle = settings.sticky && settings.inputType != KbSettings.INPUT_JOYSTICK;
			if (selected != null && !popup && !stickyNeedle)
			{
				PointF2 p = dirPoint(selected);
				paint.setColor(palette.glyphSelectedBackground);
				c.drawCircle(p.x, p.y, 35, paint);
			}

			// needle
			if (needleLength > 0 || doubleLength > 0)
			{
				c.save();
				c.rotate(-needleAngle + 90);
				paint.setColor(palette.tondosCavity);
				float len = Math.max(needleLength, doubleLength / 2);
				if (needleLength > 0) c.drawRoundRect(new RectF(-20, -needleLength, 20, 0), 20, 20, paint);
				if (doubleLength > 0) c.drawRoundRect(new RectF(-20, -doubleLength / 2, 20, doubleLength / 2), 20, 20, paint);
				c.restore();
				paint.setColor(palette.tondosCavity);
				c.drawCircle(0, 0, 30, paint);
			}

			// glyphs
			textPaint.setTextSize(50);
			for (Dir d : Dir.values())
			{
				if (d == Dir.NORTH || d == Dir.SOUTH) continue;
				String g = displayGlyph(d);
				if (g.isEmpty()) continue;
				PointF2 p = dirPoint(d);
				int color = glyphColor(g, d);
				if (selected == d) color = palette.glyphSelected;
				textPaint.setColor(color);
				textPaint.setTypeface(g.equals("␣") ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
				drawCenteredText(c, g, p.x, p.y);
			}
			c.restore();
		}

		private int glyphColor(String g, Dir d)
		{
			if (tempLabels.containsKey(d)) return palette.glyphConsonant;
			int cp = g.codePointAt(0);
			if (Character.isLetter(cp))
			{
				return "AEIOUaeiou".indexOf(cp) >= 0 ? palette.glyphVowel : palette.glyphConsonant;
			}
			return palette.glyphSymbol;
		}

		float glyphX(Dir d)
		{
			return cx + dirPoint(d).x + jx;
		}

		float glyphY(Dir d)
		{
			return cy + dirPoint(d).y + jy;
		}
	}

	private static class PointF2
	{
		final float x, y;

		PointF2(float x, float y)
		{
			this.x = x;
			this.y = y;
		}
	}

	private static PointF2 dirPoint(Dir d)
	{
		final float h = 86.60254f;
		switch (d)
		{
			case EAST:
				return new PointF2(GLYPH_RADIUS, 0);
			case NORTH_EAST:
				return new PointF2(GLYPH_RADIUS / 2, -h);
			case NORTH_WEST:
				return new PointF2(-GLYPH_RADIUS / 2, -h);
			case WEST:
				return new PointF2(-GLYPH_RADIUS, 0);
			case SOUTH_WEST:
				return new PointF2(-GLYPH_RADIUS / 2, h);
			case SOUTH_EAST:
				return new PointF2(GLYPH_RADIUS / 2, h);
			default:
				return new PointF2(0, 0);
		}
	}

	/** The cursor stick in the top bar. */
	private class CursorStick extends Elem
	{
		boolean tracking;
		float startX, startY;
		float jx, jy;
		Dir selected;
		boolean firstDirectionSelected;
		Dir previous;
		long lastSubmit;
		float delay = 250;

		@Override
		boolean hit(float x, float y)
		{
			return Math.abs(x - cx) <= 58 && Math.abs(y - cy) <= 58;
		}

		@Override
		void draw(Canvas c)
		{
			paint.setColor(palette.shadow);
			c.drawCircle(cx, cy + 8, BUTTON_RADIUS, paint);
			c.save();
			c.translate(cx + jx, cy + jy);
			paint.setColor(palette.barDirectionalBackground);
			c.drawCircle(0, 0, BUTTON_RADIUS, paint);
			drawIcon(c, R.drawable.ic_move, 0, 0, 100, palette.barDirectionalIcon, false);
			c.restore();
		}

		@Override
		void down(float x, float y, long now)
		{
			tracking = true;
			startX = x;
			startY = y;
			delay = 250;
			lastSubmit = 0;
			firstDirectionSelected = true;
			selected = null;
			vibrate();
		}

		@Override
		void move(float x, float y, long now)
		{
			if (!tracking) return;
			float distance = (float) Math.hypot(x - startX, y - startY);
			float angle = StickMath.angle(startX, startY, x, y, false);
			angle = StickMath.closestMultiple(angle, 360 / Dir.CURSOR_SECTORS.length);
			if (angle >= 360) angle = 0;
			float d = Math.min(distance * 0.5f, 116 * 0.09f);
			float ang = (float) Math.toRadians(angle);
			jx = d * (float) Math.cos(ang);
			jy = -d * (float) Math.sin(ang);

			selected = null;
			if (distance > 116 * 0.025f) selected = StickMath.sector(angle, Dir.CURSOR_SECTORS);
			if (!firstDirectionSelected && previous != selected) vibrate();
			previous = selected;
			if (selected != null && selected != Dir.CENTER) firstDirectionSelected = false;
			float maxSpeedDistance = virtualWidth * 0.5f;
			float t = Math.min(1f, distance / maxSpeedDistance);
			delay = 250 + (10 - 250) * t;
			submit(now);
		}

		@Override
		void tick(long now)
		{
			if (tracking) submit(now);
		}

		private void submit(long now)
		{
			if (selected == null) return;
			if (now - lastSubmit <= delay) return;
			lastSubmit = now;
			NativeInterface ni = NativeInterface.getInstance();
			switch (selected)
			{
				case WEST:
				case SOUTH_WEST:
				case NORTH_WEST:
					ni.MoveCursorLeft(1);
					break;
				case EAST:
				case SOUTH_EAST:
				case NORTH_EAST:
					ni.MoveCursorRight(1);
					break;
				case NORTH:
					ni.MoveCursorUp(1);
					break;
				case SOUTH:
					ni.MoveCursorDown(1);
					break;
				default:
					break;
			}
			selected = null;
		}

		@Override
		void up(float x, float y, long now)
		{
			cancel();
		}

		@Override
		void cancel()
		{
			tracking = false;
			selected = null;
			jx = jy = 0;
		}
	}

	/** Backspace: deletes on press, repeats when held, deletes words on a left swipe. */
	private class BackspaceBtn extends Btn
	{
		boolean deletingWords;
		long lastErase;
		float startX;

		BackspaceBtn()
		{
			super(R.drawable.ic_back, null);
		}

		@Override
		void down(float x, float y, long now)
		{
			pressed = true;
			downTime = now;
			lastErase = 0;
			deletingWords = false;
			startX = x;
			vibrate();
		}

		@Override
		void move(float x, float y, long now)
		{
			if (!deletingWords && startX - x > 120)
			{
				deletingWords = true;
				vibrate();
				deleteLastWord();
			}
		}

		@Override
		void tick(long now)
		{
			if (!pressed || deletingWords) return;
			if (now - downTime < 500) return;
			if (now - lastErase > 50)
			{
				erase();
				lastErase = now;
			}
		}

		@Override
		void up(float x, float y, long now)
		{
			if (pressed && now - downTime < 500 && !deletingWords) erase();
			pressed = false;
		}

		private void erase()
		{
			NativeInterface.getInstance().CommitBackspace();
			precedingCharacter = NativeInterface.getInstance().GetPrecedingCharacter(1);
			lastCommitWasEmoji = false;
		}

		private boolean isBreak(String s)
		{
			if (s == null || s.isEmpty()) return false;
			int cp = s.codePointAt(0);
			int type = Character.getType(cp);
			boolean punctuation = type == Character.CONNECTOR_PUNCTUATION || type == Character.DASH_PUNCTUATION
					|| type == Character.START_PUNCTUATION || type == Character.END_PUNCTUATION
					|| type == Character.INITIAL_QUOTE_PUNCTUATION || type == Character.FINAL_QUOTE_PUNCTUATION
					|| type == Character.OTHER_PUNCTUATION;
			return punctuation || cp == ' ' || cp == '\n';
		}

		private void deleteLastWord()
		{
			NativeInterface ni = NativeInterface.getInstance();
			String preceding = ni.GetPrecedingCharacter(1);
			int guard = 0;
			while (!preceding.isEmpty() && isBreak(preceding) && guard++ < 200)
			{
				ni.CommitBackspace();
				preceding = ni.GetPrecedingCharacter(1);
			}
			while (!preceding.isEmpty() && !isBreak(preceding) && guard++ < 400)
			{
				ni.CommitBackspace();
				preceding = ni.GetPrecedingCharacter(1);
			}
			precedingCharacter = preceding;
		}
	}

	private void buildObjects()
	{
		tondos.clear();
		mainButtons.clear();
		barButtons.clear();
		ctrlButtons.clear();
		skinButtons.clear();
		permanentButtons.clear();
		capsules.clear();
		final NativeInterface ni = NativeInterface.getInstance();

		for (CharacterMap.Stick s : CharacterMap.Stick.values()) tondos.add(new Tondo(s, false));
		// landscape duplicates of the centre tondos
		tondos.add(new Tondo(CharacterMap.Stick.TOP_CENTER, true));
		tondos.add(new Tondo(CharacterMap.Stick.BOTTOM_CENTER, true));

		shiftBtn = permanent(R.drawable.ic_shift, this::onShiftPressed);
		shiftBtn2 = permanent(R.drawable.ic_shift, this::onShiftPressed);
		symbolsBtn = permanent(R.drawable.ic_symbols, this::onSymbolsPressed);
		symbolsBtn2 = permanent(R.drawable.ic_symbols, this::onSymbolsPressed);
		symbols2Btn = permanent(R.drawable.ic_symbols2, this::onSymbols2Pressed);
		symbols2Btn2 = permanent(R.drawable.ic_symbols2, this::onSymbols2Pressed);
		backspaceBtn = new BackspaceBtn();
		backspaceBtn2 = new BackspaceBtn();
		doneBtn = permanent(R.drawable.ic_done_newline, this::onDonePressed);
		doneBtn2 = permanent(R.drawable.ic_done_newline, this::onDonePressed);
		permanentButtons.add(backspaceBtn);
		permanentButtons.add(backspaceBtn2);

		Capsule c1 = new Capsule(), c2 = new Capsule(), c3 = new Capsule(), c4 = new Capsule();
		c1.a = shiftBtn;
		c1.b = symbolsBtn;
		c2.a = shiftBtn2;
		c2.b = symbolsBtn2;
		c3.a = backspaceBtn;
		c3.b = doneBtn;
		c4.a = backspaceBtn2;
		c4.b = doneBtn2;
		capsules.add(c1);
		capsules.add(c2);
		capsules.add(c3);
		capsules.add(c4);

		hideBtn = new Btn(R.drawable.ic_hide, () -> {
			settings.setTopBarHidden(!settings.topBarHidden);
			host.onTopBarHiddenChanged(settings.topBarHidden);
			invalidateLayout();
		});
		hideBtn.w = 64;
		hideBtn.h = 58;
		hideBtn.showBackground = false;
		hideBtn.triggerOnDown = false;

		// ---- top bar
		cursorStick = new CursorStick();
		cursorStick2 = new CursorStick();
		barButtons.add(bar(R.drawable.ic_macro, () -> host.onViewRequested(KeyboardRootView.Content.MACROS)));
		barButtons.add(bar(R.drawable.ic_settings, ni::OpenKeyboardOptions));
		barButtons.add(bar(R.drawable.ic_keys, ni::OpenSystemKeyboardChooser));
		barButtons.add(bar(R.drawable.ic_ctrl, () -> setBar(Bar.CTRL)));
		barButtons.add(bar(R.drawable.ic_microphone, () -> host.onViewRequested(KeyboardRootView.Content.MIC)));
		barButtons.add(bar(R.drawable.ic_emote, () -> host.onViewRequested(KeyboardRootView.Content.EMOJI)));

		ctrlButtons.add(bar(R.drawable.ic_ctrl, () -> setBar(Bar.MAIN)));
		ctrlButtons.add(bar(R.drawable.ic_ctrl_copy, ni::Copy));
		ctrlButtons.add(bar(R.drawable.ic_ctrl_paste, ni::Paste));
		ctrlButtons.add(bar(R.drawable.ic_ctrl_cut, ni::Cut));
		ctrlButtons.add(bar(R.drawable.ic_ctrl_select, () -> {
			if (host.isPro()) ni.SelectAll();
			else
			{
				ni.showProRequiredToast();
				ni.OpenKeyboardOptions();
			}
		}));
		ctrlButtons.add(bar(R.drawable.ic_ctrl_undo, ni::Undo));
		ctrlButtons.add(bar(R.drawable.ic_ctrl_redo, ni::Redo));

		emojiBackBtn = bar(R.drawable.ic_emote, () -> host.onViewRequested(KeyboardRootView.Content.EMOJI));
		skinButtons.add(emojiBackBtn);
		String[] tones = {"", "🏻", "🏼", "🏽", "🏾", "🏿"};
		for (int i = 0; i < tones.length; i++)
		{
			final int toneIndex = i;
			Btn b = new SkinBtn(toneIndex);
			skinButtons.add(b);
		}
	}

	/** Skin tone button: a hand tinted... drawn as colored circle (system emoji fonts do the rest). */
	private class SkinBtn extends Btn
	{
		final int toneIndex;
		final int[] colors = {0xFFFFCC22, 0xFFF7D7C4, 0xFFD8B094, 0xFFB78A66, 0xFF8A5D3B, 0xFF4F3322};

		SkinBtn(int toneIndex)
		{
			super(0, () -> EmojiStore.get(null).setSkinTone(toneIndex));
			this.toneIndex = toneIndex;
			this.w = 80;
			this.radius = 34;
			this.onLongPress = () -> EmojiStore.get(null).setSkinTone2(toneIndex);
		}

		@Override
		void draw(Canvas c)
		{
			EmojiStore store = EmojiStore.get(null);
			boolean sel1 = store.getSkinTone() == toneIndex;
			boolean sel2 = store.getSkinTone2() == toneIndex;
			if (sel1 || sel2)
			{
				paint.setColor(palette.barButtonBackground);
				c.drawCircle(cx, cy, 46, paint);
			}
			paint.setColor(colors[toneIndex]);
			if (sel1 && sel2 || !sel1 && !sel2) c.drawCircle(cx, cy, radius, paint);
			else
			{
				// split circle: left half is tone 1, right half tone 2
				RectF r = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
				paint.setColor(sel1 ? colors[toneIndex] : colors[toneIndex]);
				c.drawArc(r, sel1 ? 90 : -90, 180, true, paint);
			}
		}
	}

	private Btn permanent(int icon, Runnable r)
	{
		Btn b = new Btn(icon, r);
		permanentButtons.add(b);
		return b;
	}

	private Btn bar(int icon, Runnable r)
	{
		return new Btn(icon, r);
	}

	// ------------------------------------------------------------------------------------------
	// Actions
	// ------------------------------------------------------------------------------------------

	private void vibrate()
	{
		if (settings.vibrationMillis > 0) NativeInterface.getInstance().Vibrate(settings.vibrationMillis);
	}

	private void setShift(ShiftState s)
	{
		if (shift == s) return;
		shift = s;
		invalidate();
	}

	private void onShiftPressed()
	{
		long now = SystemClock.uptimeMillis();
		if (now - lastShiftTime < 500)
		{
			setShift(ShiftState.DOUBLE_PRESSED);
			lastShiftTime = 0;
			return;
		}
		lastShiftTime = now;
		setShift(shift == ShiftState.NOT_PRESSED ? ShiftState.PRESSED : ShiftState.NOT_PRESSED);
	}

	private void onSymbolsPressed()
	{
		layout = layout == CharacterMap.Layout.MAIN ? CharacterMap.Layout.SYMBOLS1 : CharacterMap.Layout.MAIN;
		invalidate();
	}

	private void onSymbols2Pressed()
	{
		layout = layout == CharacterMap.Layout.SYMBOLS1 ? CharacterMap.Layout.SYMBOLS2 : CharacterMap.Layout.SYMBOLS1;
		invalidate();
	}

	private void onDonePressed()
	{
		NativeInterface.getInstance().CommitDone();
		lastCommitWasEmoji = false;
		precedingCharacter = NativeInterface.getInstance().GetPrecedingCharacter(1);
		onDoneCommitted();
	}

	private void onGlyphCommitting(String preceding, String selected)
	{
		if (preceding == null || preceding.isEmpty() || selected.isEmpty()) return;
		if (settings.disableAutomaticCapitalization) return;
		if (shift == ShiftState.NOT_PRESSED && ".?!¿¡‽".contains(preceding)
				&& (selected.equals(" ") || selected.equals("\n")))
			setPressedAfterNextCommit = true;
	}

	private void onGlyphCommitted(String glyph)
	{
		if (shift == ShiftState.PRESSED) setShift(ShiftState.NOT_PRESSED);
		if (!settings.disableAutomaticCapitalization && setPressedAfterNextCommit)
		{
			setPressedAfterNextCommit = false;
			setShift(ShiftState.PRESSED);
		}
		if (glyph.equals(" ") && layout != CharacterMap.Layout.MAIN)
		{
			layout = CharacterMap.Layout.MAIN;
		}
		invalidate();
	}

	// ------------------------------------------------------------------------------------------
	// Positioning
	// ------------------------------------------------------------------------------------------

	private float ax(char anchor, float x)
	{
		switch (anchor)
		{
			case 'R':
				return virtualWidth + x;
			case 'C':
				return virtualWidth / 2 + x;
			default:
				return x;
		}
	}

	private float ay(float y)
	{
		return contentTop + BOTTOM_HEIGHT / 2 - y;
	}

	private void place(Elem e, char anchor, float x, float y)
	{
		e.cx = ax(anchor, x);
		e.cy = ay(y);
	}

	private void positionObjects()
	{
		// tondos
		for (Tondo t : tondos)
		{
			t.visible = !t.landscapeOnly || landscape;
			switch (t.position)
			{
				case TOP_LEFT:
					place(t, 'L', 313, 130);
					break;
				case BOTTOM_LEFT:
					place(t, 'L', 162, -131);
					break;
				case TOP_RIGHT:
					place(t, 'R', -162, 130);
					break;
				case BOTTOM_RIGHT:
					place(t, 'R', -313, -131);
					break;
				case TOP_CENTER:
					if (t.landscapeOnly) place(t, 'R', -464, 130);
					else if (landscape) place(t, 'L', 616, 128);
					else place(t, 'C', 76, 130);
					break;
				case BOTTOM_CENTER:
					if (t.landscapeOnly) place(t, 'R', -615.5f, -131);
					else if (landscape) place(t, 'L', 464.5f, -134);
					else place(t, 'C', -75.5f, -131);
					break;
			}
		}

		if (landscape)
		{
			place(shiftBtn, 'L', 90, 210);
			place(symbolsBtn, 'L', 90, 76);
			place(symbols2Btn, 'L', 90, 210);
			place(shiftBtn2, 'R', -680, 210);
			place(symbolsBtn2, 'R', -680, 76);
			place(symbols2Btn2, 'R', -680, 210);
			place(backspaceBtn, 'R', -90, -82);
			place(doneBtn, 'R', -90, -216);
			place(backspaceBtn2, 'L', 680, -82);
			place(doneBtn2, 'L', 680, -216);
		}
		else
		{
			place(shiftBtn, 'L', 90, 210);
			place(symbolsBtn, 'L', 90, 76);
			place(symbols2Btn, 'L', 90, 210);
			place(backspaceBtn, 'R', -90, -82);
			place(doneBtn, 'R', -90, -216);
		}

		// hide button sits next to the bar
		if (settings.topBarWentBottom)
		{
			place(hideBtn, 'L', 32, -262.5f);
			hideBtn.flipV = !settings.topBarHidden;
		}
		else
		{
			place(hideBtn, 'R', -32, 262.5f);
			hideBtn.flipV = settings.topBarHidden;
		}
		// the icon is a chevron pointing up: it points towards the bar when hiding
		hideBtn.flipV = settings.topBarWentBottom ? settings.topBarHidden : !settings.topBarHidden;

		layoutBar();
	}

	private void layoutBar()
	{
		float cy = barTop + getBarHeightUnits() / 2;
		float barCy = settings.topBarHidden ? cy : barTop + BAR_HEIGHT / 2;
		List<Elem> row = new ArrayList<>();
		if (bar == Bar.MAIN)
		{
			row.add(cursorStick);
			row.addAll(barButtons.subList(0, 3));
			if (landscape) row.add(null);
			row.addAll(barButtons.subList(3, barButtons.size()));
			if (landscape) row.add(cursorStick2);
		}
		else if (bar == Bar.CTRL)
		{
			row.addAll(ctrlButtons);
		}
		else
		{
			row.addAll(skinButtons);
		}
		int n = row.size();
		for (int i = 0; i < n; i++)
		{
			Elem e = row.get(i);
			if (e == null) continue;
			e.cx = virtualWidth * (i + 0.5f) / n;
			e.cy = barCy;
		}
	}

	private List<Elem> activeBarElems()
	{
		List<Elem> list = new ArrayList<>();
		if (settings.topBarHidden) return list;
		if (bar == Bar.MAIN)
		{
			list.add(cursorStick);
			list.addAll(barButtons);
			if (landscape) list.add(cursorStick2);
		}
		else if (bar == Bar.CTRL) list.addAll(ctrlButtons);
		else list.addAll(skinButtons);
		return list;
	}

	// ------------------------------------------------------------------------------------------
	// Drawing
	// ------------------------------------------------------------------------------------------

	private Bitmap bitmap(int res)
	{
		Bitmap b = bitmaps.get(res);
		if (b == null)
		{
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inScaled = false;
			b = BitmapFactory.decodeResource(getResources(), res, o);
			bitmaps.put(res, b);
		}
		return b;
	}

	private void drawIcon(Canvas c, int res, float x, float y, float size, int color, boolean flipV)
	{
		if (res == 0) return;
		Bitmap b = bitmap(res);
		if (b == null) return;
		paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
		paint.setFilterBitmap(true);
		c.save();
		c.translate(x, y);
		if (flipV) c.scale(1, -1);
		float k = size / Math.max(b.getWidth(), b.getHeight());
		RectF dst = new RectF(-b.getWidth() * k / 2, -b.getHeight() * k / 2, b.getWidth() * k / 2, b.getHeight() * k / 2);
		c.drawBitmap(b, null, dst, paint);
		c.restore();
		paint.setColorFilter(null);
	}

	private void drawCenteredText(Canvas c, String s, float x, float y)
	{
		Paint.FontMetrics fm = textPaint.getFontMetrics();
		c.drawText(s, x, y - (fm.ascent + fm.descent) / 2, textPaint);
	}

	private void refreshDynamicIcons()
	{
		int shiftIcon = shift == ShiftState.DOUBLE_PRESSED ? R.drawable.ic_shift_double_press : R.drawable.ic_shift;
		int shiftColor = shift == ShiftState.NOT_PRESSED ? palette.permanentButtonIcon : palette.glyphSelected;
		for (Btn b : new Btn[]{shiftBtn, shiftBtn2})
		{
			b.icon = shiftIcon;
			b.iconColor = shiftColor;
			b.bgColor = palette.permanentButtonBackground;
		}
		boolean symbols = layout != CharacterMap.Layout.MAIN;
		for (Btn b : new Btn[]{symbolsBtn, symbolsBtn2})
		{
			b.icon = symbols ? R.drawable.ic_abc : R.drawable.ic_symbols;
			b.iconColor = palette.permanentButtonIcon;
			b.bgColor = palette.permanentButtonBackground;
		}
		for (Btn b : new Btn[]{symbols2Btn, symbols2Btn2})
		{
			b.icon = layout == CharacterMap.Layout.SYMBOLS2 ? R.drawable.ic_symbols : R.drawable.ic_symbols2;
			b.iconColor = palette.permanentButtonIcon;
			b.bgColor = palette.permanentButtonBackground;
		}
		for (Btn b : new Btn[]{backspaceBtn, backspaceBtn2})
		{
			b.iconColor = palette.permanentButtonIcon;
			b.bgColor = palette.permanentButtonBackground;
		}
		int doneIcon;
		switch (doneFlag)
		{
			case KeyboardActionListener.DONE_ACTION_FLAG_DONE:
				doneIcon = R.drawable.ic_done_done;
				break;
			case KeyboardActionListener.DONE_ACTION_FLAG_SEARCH:
				doneIcon = R.drawable.ic_done_search;
				break;
			case KeyboardActionListener.DONE_ACTION_FLAG_SEND:
				doneIcon = R.drawable.ic_done_send;
				break;
			default:
				doneIcon = R.drawable.ic_done_newline;
		}
		for (Btn b : new Btn[]{doneBtn, doneBtn2})
		{
			b.icon = doneIcon;
			b.iconColor = palette.doneIcon;
			b.bgColor = palette.doneBackground;
		}
		for (Capsule c : capsules)
		{
			c.color = (c.b == doneBtn || c.b == doneBtn2) ? palette.permanentButtonBackground : palette.permanentButtonBackground;
		}
		hideBtn.iconColor = palette.permanentButtonIcon;
		for (Btn b : barButtons) { b.bgColor = palette.barButtonBackground; b.iconColor = palette.barButtonIcon; }
		for (Btn b : ctrlButtons) { b.bgColor = palette.barButtonBackground; b.iconColor = palette.barButtonIcon; }
		emojiBackBtn.bgColor = palette.barButtonBackground;
		emojiBackBtn.iconColor = palette.barButtonIcon;
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		if (scale <= 0) computeGeometry(getWidth(), getHeight());
		refreshDynamicIcons();
		canvas.drawColor(palette.keyboardBackground);
		canvas.save();
		canvas.scale(scale, scale);

		// ---- action bar
		float barH = getBarHeightUnits();
		paint.setColor(palette.barBackground);
		canvas.drawRect(0, barTop, virtualWidth, barTop + barH, paint);
		layoutBar();
		for (Elem e : activeBarElems()) e.draw(canvas);

		boolean mainShown = host.getContent() == KeyboardRootView.Content.MAIN;
		if (mainShown)
		{
			for (Capsule cap : capsules) if (capsuleVisible(cap)) cap.draw(canvas);
			for (Btn b : visiblePermanentButtons()) b.draw(canvas);
			for (Tondo t : tondos) if (t.visible) t.draw(canvas);
			// popup preview on top of everything
			for (Tondo t : tondos) if (t.visible && t.selected != null && settings.showPopupPreview) drawPopup(canvas, t);
		}
		hideBtn.draw(canvas);
		canvas.restore();
	}

	private boolean capsuleVisible(Capsule c)
	{
		boolean second = c.a == shiftBtn2 || c.a == backspaceBtn2;
		if (second && !landscape) return false;
		boolean isShiftCapsule = c.a == shiftBtn || c.a == shiftBtn2;
		return true && (isShiftCapsule || true);
	}

	private List<Btn> visiblePermanentButtons()
	{
		List<Btn> list = new ArrayList<>();
		boolean symbols = layout != CharacterMap.Layout.MAIN;
		// shift / symbols group
		if (!symbols) list.add(shiftBtn);
		else list.add(layout == CharacterMap.Layout.SYMBOLS1 || layout == CharacterMap.Layout.SYMBOLS2 ? symbols2Btn : shiftBtn);
		list.add(symbolsBtn);
		list.add(backspaceBtn);
		list.add(doneBtn);
		if (landscape)
		{
			if (!symbols) list.add(shiftBtn2);
			else list.add(symbols2Btn2);
			list.add(symbolsBtn2);
			list.add(backspaceBtn2);
			list.add(doneBtn2);
		}
		return list;
	}

	private void drawPopup(Canvas c, Tondo t)
	{
		Dir d = t.selected;
		String text = t.glyph(d);
		if (text.isEmpty()) return;
		if (text.equals(" ")) text = "␣";
		float gx = t.glyphX(d), gy = t.glyphY(d);
		boolean small = (d == Dir.NORTH_WEST || d == Dir.NORTH_EAST || d == Dir.NORTH) && t.cy - TONDO_RADIUS < contentTop + 30
				&& (settings.topBarHidden || settings.topBarWentBottom);
		float w = small ? 100 : 130, h = small ? 100 : 195;
		Bitmap bmp = bitmap(small ? R.drawable.ic_preview_popup_small_bg : R.drawable.ic_preview_popup_bg);
		float left = gx - w / 2;
		float top = small ? gy - h - 10 : gy - h - 5;
		// keep inside the keyboard
		left = Math.max(2, Math.min(virtualWidth - w - 2, left));
		if (top < 0) top = 0;
		RectF dst = new RectF(left, top, left + w, top + h);
		paint.setColorFilter(new PorterDuffColorFilter(palette.keyboardBackground == Color.WHITE ? Color.WHITE : palette.tondosInterior, PorterDuff.Mode.SRC_IN));
		paint.setFilterBitmap(true);
		c.drawBitmap(bmp, null, dst, paint);
		paint.setColorFilter(null);
		textPaint.setColor(palette.glyphSelected);
		textPaint.setTextSize(small ? 60 : 80);
		textPaint.setTypeface(Typeface.DEFAULT);
		drawCenteredText(c, text, left + w / 2, small ? top + h / 2 : top + w / 2 + 5);
	}

	// ------------------------------------------------------------------------------------------
	// Touch
	// ------------------------------------------------------------------------------------------

	private Elem hitTest(float x, float y)
	{
		for (Elem e : activeBarElems()) if (e.hit(x, y)) return e;
		if (hideBtn.hit(x, y)) return hideBtn;
		if (host.getContent() != KeyboardRootView.Content.MAIN) return null;
		for (Btn b : visiblePermanentButtons()) if (b.hit(x, y)) return b;
		for (int i = tondos.size() - 1; i >= 0; i--)
		{
			Tondo t = tondos.get(i);
			if (t.visible && t.hit(x, y)) return t;
		}
		return null;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		int action = ev.getActionMasked();
		long now = SystemClock.uptimeMillis();
		switch (action)
		{
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
			{
				int idx = ev.getActionIndex();
				float x = ev.getX(idx) / scale, y = ev.getY(idx) / scale;
				Elem e = hitTest(x, y);
				int id = ev.getPointerId(idx);
				if (e != null)
				{
					pointerTargets.put(id, e);
					e.down(x, y, now);
					handler.removeCallbacks(ticker);
					handler.post(ticker);
				}
				invalidate();
				return true;
			}
			case MotionEvent.ACTION_MOVE:
			{
				for (int i = 0; i < ev.getPointerCount(); i++)
				{
					Elem e = pointerTargets.get(ev.getPointerId(i));
					if (e != null) e.move(ev.getX(i) / scale, ev.getY(i) / scale, now);
				}
				invalidate();
				return true;
			}
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
			{
				int idx = ev.getActionIndex();
				int id = ev.getPointerId(idx);
				Elem e = pointerTargets.get(id);
				pointerTargets.remove(id);
				if (e != null) e.up(ev.getX(idx) / scale, ev.getY(idx) / scale, now);
				invalidate();
				return true;
			}
			case MotionEvent.ACTION_CANCEL:
			{
				for (int i = 0; i < pointerTargets.size(); i++) pointerTargets.valueAt(i).cancel();
				pointerTargets.clear();
				invalidate();
				return true;
			}
		}
		return super.onTouchEvent(ev);
	}
}
