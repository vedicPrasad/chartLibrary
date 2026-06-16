package com.vedicrishiastro.vedicchart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VedicChartView extends View {
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint signTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint planetTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF chartBounds = new RectF();
    private final RectF textBounds = new RectF();
    private final List<ZodiacHouse> houses = new ArrayList<>();

    private ChartStyle chartStyle = ChartStyle.NORTH;
    private ChartTheme chartTheme = ChartTheme.light();
    private int chartSizeInsetDp = 5;

    public VedicChartView(Context context) {
        super(context);
        init(null);
    }

    public VedicChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public VedicChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public void setChartData(List<ZodiacHouse> values) {
        houses.clear();
        if (values != null) {
            houses.addAll(values);
        }
        invalidate();
    }

    public void setChartStyle(ChartStyle value) {
        chartStyle = value == null ? ChartStyle.NORTH : value;
        invalidate();
    }

    public void setChartStyle(String value) {
        setChartStyle(ChartStyle.from(value));
    }

    public void setChartTheme(ChartTheme value) {
        chartTheme = value == null ? ChartTheme.light() : value;
        configurePaints();
        invalidate();
    }

    public List<ZodiacHouse> getChartData() {
        return Collections.unmodifiableList(houses);
    }

    public ChartStyle getChartStyle() {
        return chartStyle;
    }

    public ChartTheme getChartTheme() {
        return chartTheme;
    }

    public void setChartSizeInsetDp(int value) {
        chartSizeInsetDp = Math.max(0, value);
        requestLayout();
        invalidate();
    }

    public int getChartSizeInsetDp() {
        return chartSizeInsetDp;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.max(0, getResources().getDisplayMetrics().widthPixels - dp(chartSizeInsetDp));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float padding = dp(12);
        float side = Math.min(getWidth(), getHeight()) - padding * 2f;
        float left = (getWidth() - side) / 2f;
        float top = (getHeight() - side) / 2f;
        chartBounds.set(left, top, left + side, top + side);

        canvas.drawRect(chartBounds, fillPaint);
        if (chartStyle == ChartStyle.SOUTH) {
            drawSouthChart(canvas, chartBounds);
        } else if (chartStyle == ChartStyle.EAST) {
            drawEastChart(canvas, chartBounds);
        } else {
            drawNorthChart(canvas, chartBounds);
        }
    }

    private void init(AttributeSet attrs) {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        readAttrs(attrs);
        configurePaints();
    }

    private void readAttrs(AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.VedicChartView);
        try {
            chartStyle = ChartStyle.from(array.getInt(R.styleable.VedicChartView_vedicChartStyle, 0));
        } finally {
            array.recycle();
        }
    }

    private void configurePaints() {
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(chartTheme.getBackgroundColor());

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dpFloat(chartTheme.getBorderWidth()));
        linePaint.setColor(chartTheme.getBorderColor());

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dpFloat(chartTheme.getBorderWidth()));
        gridPaint.setColor(gridLineColor(chartTheme.getBackgroundColor()));

        signTextPaint.setTextAlign(Paint.Align.CENTER);
        signTextPaint.setColor(chartTheme.getSignTextColor());
        signTextPaint.setTextSize(sp(chartTheme.getSignTextSizeSp()));

        planetTextPaint.setTextAlign(Paint.Align.CENTER);
        planetTextPaint.setFakeBoldText(true);
        planetTextPaint.setColor(chartTheme.getPlanetTextColor());
        planetTextPaint.setTextSize(sp(chartTheme.getPlanetTextSizeSp()));
    }

    private void drawNorthChart(Canvas canvas, RectF bounds) {
        float left = bounds.left;
        float top = bounds.top;
        float right = bounds.right;
        float bottom = bounds.bottom;
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();
        float midTop = top;
        float midRight = right;
        float midBottom = bottom;
        float midLeft = left;

        canvas.drawRect(bounds, linePaint);
        canvas.drawLine(left, top, right, bottom, gridPaint);
        canvas.drawLine(right, top, left, bottom, gridPaint);

        Path diamond = new Path();
        diamond.moveTo(centerX, midTop);
        diamond.lineTo(midRight, centerY);
        diamond.lineTo(centerX, midBottom);
        diamond.lineTo(midLeft, centerY);
        diamond.close();
        canvas.drawPath(diamond, gridPaint);

        float[][] boxes = {
                {0.38f, 0.05f, 0.62f, 0.24f}, {0.58f, 0.13f, 0.86f, 0.36f},
                {0.76f, 0.38f, 0.96f, 0.62f}, {0.58f, 0.64f, 0.86f, 0.87f},
                {0.38f, 0.76f, 0.62f, 0.95f}, {0.14f, 0.64f, 0.42f, 0.87f},
                {0.04f, 0.38f, 0.24f, 0.62f}, {0.14f, 0.13f, 0.42f, 0.36f},
                {0.36f, 0.26f, 0.64f, 0.44f}, {0.56f, 0.40f, 0.76f, 0.60f},
                {0.36f, 0.56f, 0.64f, 0.74f}, {0.24f, 0.40f, 0.44f, 0.60f}
        };
        drawHouseTexts(canvas, bounds, boxes, false);
    }

    private void drawSouthChart(Canvas canvas, RectF bounds) {
        float cell = bounds.width() / 4f;
        canvas.drawRect(bounds, linePaint);
        for (int index = 1; index < 4; index++) {
            canvas.drawLine(bounds.left + index * cell, bounds.top, bounds.left + index * cell, bounds.bottom, gridPaint);
            canvas.drawLine(bounds.left, bounds.top + index * cell, bounds.right, bounds.top + index * cell, gridPaint);
        }

        fillCenter(canvas, bounds, cell);
        float[][] boxes = {
                {0.25f, 0.00f, 0.50f, 0.25f}, {0.50f, 0.00f, 0.75f, 0.25f},
                {0.75f, 0.00f, 1.00f, 0.25f}, {0.75f, 0.25f, 1.00f, 0.50f},
                {0.75f, 0.50f, 1.00f, 0.75f}, {0.75f, 0.75f, 1.00f, 1.00f},
                {0.50f, 0.75f, 0.75f, 1.00f}, {0.25f, 0.75f, 0.50f, 1.00f},
                {0.00f, 0.75f, 0.25f, 1.00f}, {0.00f, 0.50f, 0.25f, 0.75f},
                {0.00f, 0.25f, 0.25f, 0.50f}, {0.00f, 0.00f, 0.25f, 0.25f}
        };
        drawHouseTexts(canvas, bounds, boxes, true);
    }

    private void drawEastChart(Canvas canvas, RectF bounds) {
        float third = bounds.width() / 3f;
        canvas.drawRect(bounds, linePaint);
        canvas.drawLine(bounds.left + third, bounds.top, bounds.left + third, bounds.bottom, gridPaint);
        canvas.drawLine(bounds.left + third * 2f, bounds.top, bounds.left + third * 2f, bounds.bottom, gridPaint);
        canvas.drawLine(bounds.left, bounds.top + third, bounds.right, bounds.top + third, gridPaint);
        canvas.drawLine(bounds.left, bounds.top + third * 2f, bounds.right, bounds.top + third * 2f, gridPaint);

        canvas.drawLine(bounds.left, bounds.top, bounds.left + third, bounds.top + third, gridPaint);
        canvas.drawLine(bounds.right, bounds.top, bounds.left + third * 2f, bounds.top + third, gridPaint);
        canvas.drawLine(bounds.right, bounds.bottom, bounds.left + third * 2f, bounds.top + third * 2f, gridPaint);
        canvas.drawLine(bounds.left, bounds.bottom, bounds.left + third, bounds.top + third * 2f, gridPaint);

        float[][] boxes = {
                {0.00f, 0.00f, 0.33f, 0.27f}, {0.33f, 0.00f, 0.67f, 0.33f},
                {0.67f, 0.00f, 1.00f, 0.27f}, {0.67f, 0.27f, 1.00f, 0.52f},
                {0.67f, 0.67f, 1.00f, 1.00f}, {0.33f, 0.67f, 0.67f, 1.00f},
                {0.00f, 0.67f, 0.33f, 1.00f}, {0.00f, 0.27f, 0.33f, 0.52f},
                {0.20f, 0.26f, 0.47f, 0.47f}, {0.53f, 0.26f, 0.80f, 0.47f},
                {0.53f, 0.53f, 0.80f, 0.80f}, {0.20f, 0.53f, 0.47f, 0.80f}
        };
        drawHouseTexts(canvas, bounds, boxes, true);
    }

    private void fillCenter(Canvas canvas, RectF bounds, float cell) {
        RectF center = new RectF(
                bounds.left + cell,
                bounds.top + cell,
                bounds.left + cell * 3f,
                bounds.top + cell * 3f
        );
        canvas.drawRect(center, fillPaint);
        canvas.drawRect(center, gridPaint);
    }

    private void drawHouseTexts(Canvas canvas, RectF bounds, float[][] boxes, boolean useSignIndexedBoxes) {
        int count = Math.min(Math.min(boxes.length, houses.size()), 12);
        for (int index = 0; index < count; index++) {
            ZodiacHouse house = houses.get(index);
            int boxIndex = useSignIndexedBoxes && house.getSign() >= 1 && house.getSign() <= 12
                    ? house.getSign() - 1
                    : index;
            float[] box = boxes[boxIndex];
            textBounds.set(
                    bounds.left + bounds.width() * box[0],
                    bounds.top + bounds.height() * box[1],
                    bounds.left + bounds.width() * box[2],
                    bounds.top + bounds.height() * box[3]
            );
            drawHouse(canvas, house, textBounds, index, useSignIndexedBoxes);
        }
    }

    private void drawHouse(Canvas canvas, ZodiacHouse house, RectF box, int houseIndex, boolean hideSignNumber) {
        String ascendant = houseIndex == 0 ? "ASC" : "";
        String sign = hideSignNumber
                ? ascendant
                : chartTheme.shouldShowSignNames()
                ? house.getSignName() + " (" + house.getSign() + ")" + (ascendant.isEmpty() ? "" : " " + ascendant)
                : String.valueOf(house.getSign()) + (ascendant.isEmpty() ? "" : " " + ascendant);
        String planets = house.getPlanetDisplayText();
        List<TextLine> lines = buildFittedLines(sign, planets, box);
        if (lines.isEmpty()) {
            return;
        }

        int saveCount = canvas.save();
        float inset = dpFloat(3);
        canvas.clipRect(box.left + inset, box.top + inset, box.right - inset, box.bottom - inset);

        float lineHeight = maxLineTextSize(lines) * 1.15f;
        float totalHeight = lineHeight * lines.size();
        float y = box.centerY() - totalHeight / 2f + lineHeight * 0.5f;
        for (TextLine line : lines) {
            canvas.drawText(line.text, box.centerX(), y + centeredTextOffset(line.paint), line.paint);
            y += lineHeight;
        }
        canvas.restoreToCount(saveCount);
    }

    private List<TextLine> buildFittedLines(String sign, String planets, RectF box) {
        float maxWidth = Math.max(1f, box.width() - dpFloat(8));
        float maxHeight = Math.max(1f, box.height() - dpFloat(8));
        float signBaseSize = sp(chartTheme.getSignTextSizeSp());
        float planetBaseSize = sp(chartTheme.getPlanetTextSizeSp());
        float minTextSize = sp(7);

        for (float scale = 1f; scale >= 0.55f; scale -= 0.05f) {
            signTextPaint.setTextSize(Math.max(minTextSize, signBaseSize * scale));
            planetTextPaint.setTextSize(Math.max(minTextSize, planetBaseSize * scale));

            List<TextLine> lines = new ArrayList<>();
            if (sign != null && !sign.trim().isEmpty()) {
                lines.addAll(wrapText(sign, signTextPaint, maxWidth, 1));
            }
            if (planets != null && !planets.isEmpty()) {
                lines.addAll(wrapText(planets, planetTextPaint, maxWidth, 2));
            }
            float lineHeight = maxLineTextSize(lines) * 1.15f;
            if (lineHeight * lines.size() <= maxHeight && allLinesFit(lines, maxWidth)) {
                return lines;
            }
        }

        signTextPaint.setTextSize(minTextSize);
        planetTextPaint.setTextSize(minTextSize);
        List<TextLine> compactLines = new ArrayList<>();
        if (sign != null && !sign.trim().isEmpty()) {
            compactLines.add(new TextLine(ellipsize(sign, signTextPaint, maxWidth), signTextPaint));
        }
        if (planets != null && !planets.isEmpty() && maxHeight >= minTextSize * 2.2f) {
            compactLines.add(new TextLine(ellipsize(planets, planetTextPaint, maxWidth), planetTextPaint));
        }
        return compactLines;
    }

    private List<TextLine> wrapText(String text, Paint paint, float maxWidth, int maxLines) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        ArrayDeque<String> words = new ArrayDeque<>();
        for (String word : text.trim().split("\\s+")) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }

        List<TextLine> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        while (!words.isEmpty() && lines.size() < maxLines) {
            String word = words.removeFirst();
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (paint.measureText(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (current.length() > 0) {
                    lines.add(new TextLine(current.toString(), paint));
                    current.setLength(0);
                    words.addFirst(word);
                } else {
                    lines.add(new TextLine(ellipsize(word, paint, maxWidth), paint));
                }
            }
        }
        if (current.length() > 0 && lines.size() < maxLines) {
            lines.add(new TextLine(current.toString(), paint));
        }
        if (!words.isEmpty() && !lines.isEmpty()) {
            TextLine last = lines.get(lines.size() - 1);
            lines.set(lines.size() - 1, new TextLine(ellipsize(last.text + "...", paint, maxWidth), paint));
        }
        return lines;
    }

    private boolean allLinesFit(List<TextLine> lines, float maxWidth) {
        for (TextLine line : lines) {
            if (line.paint.measureText(line.text) > maxWidth) {
                return false;
            }
        }
        return true;
    }

    private float maxLineTextSize(List<TextLine> lines) {
        float max = sp(7);
        for (TextLine line : lines) {
            max = Math.max(max, line.paint.getTextSize());
        }
        return max;
    }

    private String ellipsize(String text, Paint paint, float maxWidth) {
        if (paint.measureText(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0 && paint.measureText(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return end == 0 ? "" : text.substring(0, end) + suffix;
    }

    private float centeredTextOffset(Paint paint) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        return -(metrics.ascent + metrics.descent) / 2f;
    }

    private int dp(float value) {
        return Math.round(dpFloat(value));
    }

    private float dpFloat(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private int gridLineColor(int backgroundColor) {
        float luminance = (
                Color.red(backgroundColor) * 0.299f
                        + Color.green(backgroundColor) * 0.587f
                        + Color.blue(backgroundColor) * 0.114f
        ) / 255f;
        return luminance < 0.45f
                ? Color.rgb(226, 232, 240)
                : Color.rgb(43, 35, 28);
    }

    private static final class TextLine {
        private final String text;
        private final Paint paint;

        private TextLine(String text, Paint paint) {
            this.text = text;
            this.paint = paint;
        }
    }
}
