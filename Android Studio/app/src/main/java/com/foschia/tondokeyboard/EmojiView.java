package com.foschia.tondokeyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Emoji picker: a scrolling grid grouped by category plus a category bar. Emojis are rendered with the system font. */
public class EmojiView extends FrameLayout
{
	private static final String RECENT = "Recently used";
	private static final String PEOPLE = "People & Body";
	private static final String[] CATEGORY_ICONS = {"🕒", "😀", "👋", "🐻", "🍔", "✈️", "⚽", "💡", "❤️"};

	public interface Callbacks
	{
		void onEmoji(String emoji);

		void onBackspace();

		Palette palette();
	}

	private static class Item
	{
		final String text;
		final boolean header;
		final String category;
		final String baseEmoji;

		Item(String text, boolean header, String category, String base)
		{
			this.text = text;
			this.header = header;
			this.category = category;
			this.baseEmoji = base;
		}
	}

	private static List<String> categoryNames;
	private static List<List<String>> categoryEmojis;

	private final Callbacks callbacks;
	private final RecyclerView grid;
	private final LinearLayout categoryBar;
	private final GridLayoutManager layoutManager;
	private final List<Item> items = new ArrayList<>();
	private final List<Integer> categoryPositions = new ArrayList<>();
	private final Paint probe = new Paint();
	private float unit = 1f;
	private int selectedCategory = 0;
	private final Runnable storeListener = this::rebuild;

	private final RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>()
	{
		@Override
		public int getItemViewType(int position)
		{
			return items.get(position).header ? 1 : 0;
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
		{
			TextView tv = new TextView(parent.getContext());
			tv.setGravity(Gravity.CENTER);
			if (viewType == 1)
			{
				tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 34 * unit);
				tv.setTypeface(Typeface.DEFAULT_BOLD);
				tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
				tv.setPadding((int) (20 * unit), (int) (10 * unit), 0, (int) (4 * unit));
				tv.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (70 * unit)));
			}
			else
			{
				tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 64 * unit);
				tv.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (110 * unit)));
			}
			return new RecyclerView.ViewHolder(tv) {};
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
		{
			final Item item = items.get(position);
			TextView tv = (TextView) holder.itemView;
			tv.setText(item.text);
			if (item.header)
			{
				tv.setTextColor(callbacks.palette().glyphConsonant);
				return;
			}
			tv.setOnClickListener(v -> {
				NativeInterface.getInstance().Vibrate(10);
				callbacks.onEmoji(item.text);
				EmojiStore.get(getContext()).use(item.text);
			});
		}

		@Override
		public int getItemCount()
		{
			return items.size();
		}
	};

	public EmojiView(Context context, Callbacks callbacks)
	{
		super(context);
		this.callbacks = callbacks;
		loadData(context);
		layoutManager = new GridLayoutManager(context, 8);
		layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup()
		{
			@Override
			public int getSpanSize(int position)
			{
				return items.get(position).header ? layoutManager.getSpanCount() : 1;
			}
		});
		grid = new RecyclerView(context);
		grid.setLayoutManager(layoutManager);
		grid.setAdapter(adapter);
		grid.setHasFixedSize(false);
		grid.addOnScrollListener(new RecyclerView.OnScrollListener()
		{
			@Override
			public void onScrolled(@NonNull RecyclerView rv, int dx, int dy)
			{
				updateSelectedCategory();
			}
		});
		addView(grid);

		categoryBar = new LinearLayout(context);
		categoryBar.setOrientation(LinearLayout.HORIZONTAL);
		categoryBar.setGravity(Gravity.CENTER_VERTICAL);
		addView(categoryBar);
	}

	private static synchronized void loadData(Context context)
	{
		if (categoryNames != null) return;
		categoryNames = new ArrayList<>();
		categoryEmojis = new ArrayList<>();
		try (InputStream in = context.getAssets().open("data/emoji_codes_list.txt");
			 BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = r.readLine()) != null)
			{
				if (line.trim().isEmpty()) continue;
				String[] parts = line.split(",");
				categoryNames.add(parts[0].trim());
				List<String> list = new ArrayList<>();
				for (int i = 1; i < parts.length; i++)
				{
					StringBuilder sb = new StringBuilder();
					for (String cp : parts[i].trim().split(" "))
					{
						if (!cp.startsWith("U+")) continue;
						try
						{
							sb.appendCodePoint(Integer.parseInt(cp.substring(2), 16));
						}
						catch (NumberFormatException ignored)
						{
						}
					}
					if (sb.length() > 0) list.add(sb.toString());
				}
				categoryEmojis.add(list);
			}
		}
		catch (IOException ignored)
		{
		}
	}

	public void setUnit(float unit)
	{
		this.unit = unit;
		rebuild();
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();
		EmojiStore.get(getContext()).addListener(storeListener);
		rebuild();
	}

	@Override
	protected void onDetachedFromWindow()
	{
		EmojiStore.get(getContext()).removeListener(storeListener);
		super.onDetachedFromWindow();
	}

	public void scrollToTop()
	{
		grid.scrollToPosition(0);
	}

	public void applyTheme()
	{
		Palette p = callbacks.palette();
		setBackgroundColor(p.keyboardBackground);
		categoryBar.setBackgroundColor(p.barButtonBackground);
		rebuild();
	}

	private boolean supports(String s)
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) return probe.hasGlyph(s);
		return true;
	}

	/** Inserts the selected skin tone modifier(s) in an emoji, when the font can show the result. */
	private String withSkinTone(String emoji, String category)
	{
		EmojiStore store = EmojiStore.get(getContext());
		int t1 = store.getSkinTone(), t2 = store.getSkinTone2();
		if (t1 == 0 && t2 == 0 || !PEOPLE.equals(category)) return emoji;
		StringBuilder out = new StringBuilder();
		int applied = 0;
		for (int i = 0; i < emoji.length(); )
		{
			int cp = emoji.codePointAt(i);
			i += Character.charCount(cp);
			out.appendCodePoint(cp);
			if (cp == 0x200D || cp == 0xFE0F || (cp >= 0x1F3FB && cp <= 0x1F3FF)) continue;
			int tone = applied == 0 ? t1 : t2;
			if (tone == 0 || i < emoji.length() && emoji.codePointAt(i) >= 0x1F3FB && emoji.codePointAt(i) <= 0x1F3FF) continue;
			String modified = new StringBuilder().appendCodePoint(cp).appendCodePoint(0x1F3FA + tone).toString();
			if (supports(modified))
			{
				out.appendCodePoint(0x1F3FA + tone);
				applied++;
			}
		}
		String result = out.toString();
		return supports(result) ? result : emoji;
	}

	private void rebuild()
	{
		items.clear();
		categoryPositions.clear();
		List<String> recent = EmojiStore.get(getContext()).mostUsed();
		categoryPositions.add(items.size());
		items.add(new Item(RECENT, true, RECENT, null));
		for (String e : recent) items.add(new Item(e, false, RECENT, e));
		for (int c = 0; c < categoryNames.size(); c++)
		{
			categoryPositions.add(items.size());
			items.add(new Item(categoryNames.get(c), true, categoryNames.get(c), null));
			for (String e : categoryEmojis.get(c))
			{
				if (!supports(e)) continue;
				items.add(new Item(withSkinTone(e, categoryNames.get(c)), false, categoryNames.get(c), e));
			}
		}
		adapter.notifyDataSetChanged();
		buildCategoryBar();
	}

	private void buildCategoryBar()
	{
		categoryBar.removeAllViews();
		Palette p = callbacks.palette();
		for (int i = 0; i < categoryPositions.size(); i++)
		{
			final int index = i;
			TextView b = new TextView(getContext());
			b.setText(i < CATEGORY_ICONS.length ? CATEGORY_ICONS[i] : "•");
			b.setGravity(Gravity.CENTER);
			b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 56 * unit);
			b.setTextColor(p.glyphConsonant);
			if (i == selectedCategory)
			{
				GradientDrawable g = new GradientDrawable();
				g.setColor(p.glyphSelectedBackground);
				g.setCornerRadius(24 * unit);
				b.setBackground(g);
			}
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
			categoryBar.addView(b, lp);
			b.setOnClickListener(v -> {
				selectedCategory = index;
				layoutManager.scrollToPositionWithOffset(categoryPositions.get(index), 0);
				buildCategoryBar();
			});
		}
		// backspace
		ImageView back = new ImageView(getContext());
		back.setImageResource(R.drawable.ic_back);
		back.setColorFilter(p.permanentButtonIcon, PorterDuff.Mode.SRC_IN);
		back.setPadding((int) (20 * unit), (int) (20 * unit), (int) (20 * unit), (int) (20 * unit));
		categoryBar.addView(back, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.1f));
		back.setOnTouchListener(new OnTouchListener()
		{
			private final Runnable repeat = new Runnable()
			{
				@Override
				public void run()
				{
					callbacks.onBackspace();
					back.postDelayed(this, 60);
				}
			};

			@Override
			public boolean onTouch(View v, MotionEvent e)
			{
				switch (e.getActionMasked())
				{
					case MotionEvent.ACTION_DOWN:
						NativeInterface.getInstance().Vibrate(10);
						callbacks.onBackspace();
						back.postDelayed(repeat, 500);
						return true;
					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_CANCEL:
						back.removeCallbacks(repeat);
						return true;
				}
				return true;
			}
		});
	}

	private void updateSelectedCategory()
	{
		int first = layoutManager.findFirstVisibleItemPosition();
		int sel = 0;
		for (int i = 0; i < categoryPositions.size(); i++) if (categoryPositions.get(i) <= first) sel = i;
		if (sel != selectedCategory)
		{
			selectedCategory = sel;
			buildCategoryBar();
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		int w = r - l, h = b - t;
		int barH = (int) (130 * unit);
		grid.layout(0, 0, w, h - barH);
		categoryBar.layout(0, h - barH, w, h);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int w = getMeasuredWidth(), h = getMeasuredHeight();
		int barH = (int) (130 * unit);
		grid.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h - barH, MeasureSpec.EXACTLY));
		categoryBar.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(barH, MeasureSpec.EXACTLY));
		int span = Math.max(6, Math.round(w / (118 * unit)));
		if (layoutManager.getSpanCount() != span) layoutManager.setSpanCount(span);
	}
}
