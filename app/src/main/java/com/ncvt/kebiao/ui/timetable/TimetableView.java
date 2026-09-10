package com.ncvt.kebiao.ui.timetable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;
import androidx.core.content.ContextCompat;
import com.ncvt.kebiao.R;
import com.ncvt.kebiao.model.Course;
import com.ncvt.kebiao.util.ColorUtils;
import com.ncvt.kebiao.util.WeekCalculator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class TimetableView extends View {
    private static final int INACTIVE_CARD = 0xFFF1F3F6;
    private static final int INACTIVE_ACCENT = 0xFFC7CCD5;
    private static final int INACTIVE_TITLE = 0xFF7C8595;
    private static final int NUM_DAYS = 7;
    private static final String[] TIME_SLOTS = {
            "1\n08:00\n08:40", "2\n08:50\n09:30", "3\n09:40\n10:20", "4\n10:30\n11:10",
            "5\n11:20\n12:00", "6\n14:40\n15:20", "7\n15:30\n16:10", "8\n16:20\n17:00",
            "9\n19:00\n19:40", "10\n19:50\n20:30", "11\n20:40\n21:20", "12"
    };
    private List<Course> courses = Collections.emptyList();
    private int currentWeekNumber = 1;
    private int todayDayOfWeek = WeekCalculator.getTodayDayOfWeek();
    private boolean deepCourseCardEnabled;
    private final List<CourseHitArea> hitAreas = new ArrayList<>();
    private final float density;
    private final int timeLabelWidth;
    private final int headerHeight;
    private final int minCellWidth;
    private final int cellHeight;
    private final int cardPadding;
    private final float cardRadius;
    private int cellWidth;
    private int scrollYOffset;
    private int maxScrollY;
    private String currentDataSignature = "";
    private boolean autoPositionPending;
    private final Paint gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headerBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint timeSlotIndexPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint timeSlotClockPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint cardTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint badgeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardAccentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final OverScroller scroller;
    private final GestureDetector gestureDetector;
    private float downX;
    private float downY;
    private boolean moved;
    private Consumer<Course> onCourseClick;
    private IntConsumer onWeekSwipe;

    public TimetableView(Context context) { this(context, null); }
    public TimetableView(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public TimetableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = getResources().getDisplayMetrics().density;
        timeLabelWidth = getResources().getDimensionPixelSize(R.dimen.time_label_width);
        headerHeight = getResources().getDimensionPixelSize(R.dimen.header_height);
        minCellWidth = getResources().getDimensionPixelSize(R.dimen.cell_width);
        cellHeight = getResources().getDimensionPixelSize(R.dimen.cell_height);
        cardPadding = getResources().getDimensionPixelSize(R.dimen.card_padding);
        cardRadius = getResources().getDimension(R.dimen.card_radius);
        cellWidth = minCellWidth;
        gridLinePaint.setColor(ContextCompat.getColor(context, R.color.grid_line));
        gridLinePaint.setStrokeWidth(getResources().getDimension(R.dimen.grid_line_width));
        gridLinePaint.setStyle(Paint.Style.STROKE);
        headerBgPaint.setColor(ContextCompat.getColor(context, R.color.header_bg));
        todayHighlightPaint.setColor(ContextCompat.getColor(context, R.color.today_highlight));
        timeSlotIndexPaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        timeSlotIndexPaint.setTextSize(sp(15));
        timeSlotIndexPaint.setFakeBoldText(true);
        timeSlotIndexPaint.setTextAlign(Paint.Align.CENTER);
        timeSlotClockPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        timeSlotClockPaint.setTextSize(sp(10.5f));
        timeSlotClockPaint.setTextAlign(Paint.Align.CENTER);
        cardTitlePaint.setTextSize(getResources().getDimension(R.dimen.course_name_text));
        cardTitlePaint.setFakeBoldText(true);
        badgePaint.setColor(0xFFB8BEC8);
        badgeTextPaint.setColor(Color.WHITE);
        badgeTextPaint.setTextSize(sp(8));
        badgeTextPaint.setFakeBoldText(true);
        badgeTextPaint.setTextAlign(Paint.Align.CENTER);
        cardShadowPaint.setColor(0x0F000000);
        scroller = new OverScroller(context);
        gestureDetector = new GestureDetector(context, new GestureListener());
        setClickable(true);
    }

    public void setOnCourseClick(Consumer<Course> listener) { onCourseClick = listener; }
    public void setOnWeekSwipe(IntConsumer listener) { onWeekSwipe = listener; }

    public void setDeepCourseCardEnabled(boolean enabled) {
        if (deepCourseCardEnabled == enabled) return;
        deepCourseCardEnabled = enabled;
        invalidate();
    }

    public void setCourses(List<Course> courses, int weekNumber) {
        String signature = buildDataSignature(courses, weekNumber);
        autoPositionPending = !signature.equals(currentDataSignature);
        currentDataSignature = signature;
        this.courses = new ArrayList<>(courses);
        currentWeekNumber = weekNumber;
        todayDayOfWeek = WeekCalculator.getTodayDayOfWeek();
        hitAreas.clear();
        if (getWidth() > 0 && getHeight() > 0) {
            updateLayoutMetrics(getWidth(), getHeight());
            applyAutoPositionIfNeeded();
        }
        invalidate();
    }

    private int contentWidth() { return NUM_DAYS * cellWidth; }
    private int contentHeight() { return TIME_SLOTS.length * cellHeight; }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(timeLabelWidth + minCellWidth * NUM_DAYS + getPaddingLeft() + getPaddingRight(),
                widthMeasureSpec);
        int height = resolveSize(headerHeight + contentHeight() + getPaddingTop() + getPaddingBottom(),
                heightMeasureSpec);
        updateLayoutMetrics(width, height);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateLayoutMetrics(w, h);
        applyAutoPositionIfNeeded();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        hitAreas.clear();
        canvas.save();
        canvas.clipRect(timeLabelWidth, headerHeight, getWidth(), getHeight());
        canvas.translate(timeLabelWidth, headerHeight - scrollYOffset);
        drawTodayHighlight(canvas);
        drawGrid(canvas);
        drawCourseCards(canvas);
        canvas.restore();
        drawFixedHeaders(canvas);
        drawFixedTimeLabels(canvas);
    }

    private void updateLayoutMetrics(int width, int height) {
        cellWidth = Math.max(1, (width - timeLabelWidth - getPaddingLeft() - getPaddingRight()) / NUM_DAYS);
        maxScrollY = Math.max(0, contentHeight() - (height - headerHeight));
        scrollYOffset = clamp(scrollYOffset, 0, maxScrollY);
    }

    private void applyAutoPositionIfNeeded() {
        if (!autoPositionPending || getWidth() == 0 || getHeight() == 0) return;
        if (!scroller.isFinished()) scroller.abortAnimation();
        List<Course> relevant = new ArrayList<>();
        List<Course> fallback = new ArrayList<>();
        for (Course course : courses) {
            if (course.dayOfWeek < 1 || course.dayOfWeek > NUM_DAYS || course.startPeriod <= 0) continue;
            fallback.add(course);
            if (WeekCalculator.matchesWeek(course.weekPattern, currentWeekNumber)) relevant.add(course);
        }
        Course focus = pickFocusCourse(relevant.isEmpty() ? fallback : relevant);
        int focusRow = Math.max(0, (focus == null ? 1 : focus.startPeriod) - 1);
        int viewportHeight = Math.max(cellHeight, getHeight() - headerHeight);
        scrollYOffset = clamp((int) (Math.max(0, focusRow - 1) * cellHeight - viewportHeight * 0.05f), 0, maxScrollY);
        autoPositionPending = false;
        invalidate();
    }

    private Course pickFocusCourse(List<Course> source) {
        Comparator<Course> order = Comparator.comparingInt((Course course) -> course.startPeriod)
                .thenComparingInt(course -> course.dayOfWeek).thenComparing(course -> course.name);
        Course today = null;
        Course first = null;
        for (Course course : source) {
            if (course.dayOfWeek == todayDayOfWeek && (today == null || course.startPeriod < today.startPeriod)) today = course;
            if (first == null || order.compare(course, first) < 0) first = course;
        }
        return today == null ? first : today;
    }

    private String buildDataSignature(List<Course> source, int week) {
        StringBuilder text = new StringBuilder().append(week).append('#');
        for (Course course : source) {
            text.append(course.id).append(':').append(course.name).append(':').append(course.dayOfWeek)
                    .append(':').append(course.startPeriod).append(':').append(course.duration)
                    .append(':').append(course.weekPattern).append('|');
        }
        return text.toString();
    }

    private void drawTodayHighlight(Canvas canvas) {
        float left = (todayDayOfWeek - 1) * cellWidth;
        canvas.drawRect(left, 0, left + cellWidth, contentHeight(), todayHighlightPaint);
    }

    private void drawGrid(Canvas canvas) {
        for (int row = 0; row <= TIME_SLOTS.length; row++) {
            float y = row * cellHeight;
            canvas.drawLine(0, y, contentWidth(), y, gridLinePaint);
        }
        for (int column = 0; column <= NUM_DAYS; column++) {
            float x = column * cellWidth;
            canvas.drawLine(x, 0, x, contentHeight(), gridLinePaint);
        }
    }

    private void drawCourseCards(Canvas canvas) {
        for (CourseLayoutEngine.CourseLayout layout : CourseLayoutEngine.layout(courses)) {
            Course course = layout.course;
            int column = course.dayOfWeek - 1;
            int row = Math.max(0, course.startPeriod - 1);
            if (column < 0 || column >= NUM_DAYS || row >= TIME_SLOTS.length) continue;
            int span = Math.min(Math.max(1, course.duration), TIME_SLOTS.length - row);
            float fullTop = row * cellHeight + cardPadding;
            float fullBottom = (row + span) * cellHeight - cardPadding;
            float laneHeight = (fullBottom - fullTop) / layout.laneCount;
            float laneGap = Math.min(2 * density, laneHeight * 0.12f);
            float top = fullTop + layout.laneIndex * laneHeight + laneGap / 2f;
            float bottom = fullTop + (layout.laneIndex + 1) * laneHeight - laneGap / 2f;
            RectF rect = new RectF(column * cellWidth + cardPadding, top,
                    (column + 1) * cellWidth - cardPadding, Math.max(top + density, bottom));
            boolean active = WeekCalculator.matchesWeek(course.weekPattern, currentWeekNumber);
            int color = !active ? INACTIVE_CARD : deepCourseCardEnabled ? vividColor(course.color) : course.color;
            RectF shadow = new RectF(rect);
            shadow.offset(density, 2 * density);
            canvas.drawRoundRect(shadow, cardRadius, cardRadius, cardShadowPaint);
            cardPaint.setColor(color);
            canvas.drawRoundRect(rect, cardRadius, cardRadius, cardPaint);
            cardAccentPaint.setColor(active ? accentColor(color) : INACTIVE_ACCENT);
            canvas.drawRoundRect(new RectF(rect.left, rect.top, rect.right,
                    rect.top + Math.min(6 * density, rect.height() * 0.14f)), cardRadius, cardRadius, cardAccentPaint);
            RectF hit = new RectF(rect);
            hit.offset(timeLabelWidth, headerHeight - scrollYOffset);
            hitAreas.add(new CourseHitArea(hit, course));
            drawCourseContent(canvas, rect, course, active, color);
        }
    }

    private void drawCourseContent(Canvas canvas, RectF rect, Course course, boolean active, int color) {
        int textColor = active ? ColorUtils.textColorForBackground(color) : INACTIVE_TITLE;
        cardTitlePaint.setColor(textColor);
        float innerLeft = rect.left + 4 * density;
        float innerTop = rect.top + (active ? 8 : 24) * density;
        int innerWidth = Math.max(1, (int) (rect.width() - 8 * density));
        canvas.save();
        canvas.clipRect(rect);
        StaticLayout title = buildLayout(course.name, cardTitlePaint, innerWidth, maxLinesFor(rect, 2));
        canvas.save();
        canvas.translate(innerLeft, innerTop);
        title.draw(canvas);
        canvas.restore();
        canvas.restore();
        if (!active) drawOutOfWeekBadge(canvas, rect);
    }

    private int maxLinesFor(RectF rect, int preferred) {
        if (rect.height() < cellHeight * 1.15f) return 1;
        if (rect.height() < cellHeight * 2.05f) return Math.min(preferred, 2);
        return preferred + 1;
    }

    private void drawOutOfWeekBadge(Canvas canvas, RectF rect) {
        float width = Math.min(34 * density, rect.width() - 8 * density);
        if (width <= 0) return;
        RectF badge = new RectF(rect.right - width - 4 * density, rect.top + 4 * density,
                rect.right - 4 * density, rect.top + 20 * density);
        canvas.drawRoundRect(badge, 8 * density, 8 * density, badgePaint);
        float y = badge.centerY() - (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2;
        canvas.drawText("非本周", badge.centerX(), y, badgeTextPaint);
    }

    private void drawFixedHeaders(Canvas canvas) {
        canvas.drawRect(0, 0, getWidth(), headerHeight, headerBgPaint);
        canvas.drawLine(0, headerHeight, getWidth(), headerHeight, gridLinePaint);
    }

    private void drawFixedTimeLabels(Canvas canvas) {
        canvas.save();
        canvas.clipRect(0, headerHeight, timeLabelWidth, getHeight());
        canvas.drawRect(0, headerHeight, timeLabelWidth, getHeight(), headerBgPaint);
        for (int row = 0; row < TIME_SLOTS.length; row++) {
            float centerY = headerHeight + row * cellHeight - scrollYOffset + cellHeight / 2f;
            if (centerY < headerHeight - cellHeight || centerY > getHeight() + cellHeight) continue;
            String[] lines = TIME_SLOTS[row].split("\n");
            float lineGap = 14 * density;
            float startY = centerY - ((lines.length - 1) * lineGap) / 2;
            for (int line = 0; line < lines.length; line++) {
                TextPaint paint = line == 0 ? timeSlotIndexPaint : timeSlotClockPaint;
                float y = startY + line * lineGap - (paint.descent() + paint.ascent()) / 2;
                canvas.drawText(lines[line], timeLabelWidth / 2f, y, paint);
            }
        }
        canvas.restore();
        canvas.drawLine(timeLabelWidth, 0, timeLabelWidth, getHeight(), gridLinePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                moved = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - downX) > 8 * density || Math.abs(event.getY() - downY) > 8 * density) moved = true;
                break;
            case MotionEvent.ACTION_UP:
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                if (Math.abs(dx) >= 52 * density && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                    gestureDetector.onTouchEvent(event);
                    if (onWeekSwipe != null) onWeekSwipe.accept(dx < 0 ? 1 : -1);
                    performClick();
                    return true;
                }
                if (!moved && event.getX() >= timeLabelWidth && event.getY() >= headerHeight) {
                    for (int i = hitAreas.size() - 1; i >= 0; i--) {
                        CourseHitArea hit = hitAreas.get(i);
                        if (!hit.rect.contains(event.getX(), event.getY())) continue;
                        gestureDetector.onTouchEvent(event);
                        performClick();
                        if (onCourseClick != null) onCourseClick.accept(hit.course);
                        return true;
                    }
                }
                performClick();
                break;
            default:
                break;
        }
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollYOffset = clamp(scroller.getCurrY(), 0, maxScrollY);
            postInvalidateOnAnimation();
        }
    }

    private StaticLayout buildLayout(String text, TextPaint paint, int width, int maxLines) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
                .setLineSpacing(0, 1.02f).setMaxLines(Math.max(1, maxLines))
                .setEllipsize(TextUtils.TruncateAt.END).build();
    }

    private int accentColor(int color) {
        return Color.argb(Color.alpha(color), (int) (Color.red(color) * 0.88f),
                (int) (Color.green(color) * 0.88f), (int) (Color.blue(color) * 0.88f));
    }

    private int vividColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.max(0, Math.min(0.9f, hsv[1] * 1.5f + 0.08f));
        hsv[2] = Math.max(0, Math.min(1, hsv[2] * 0.97f + 0.03f));
        return Color.HSVToColor(Color.alpha(color), hsv);
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics());
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static final class CourseHitArea {
        final RectF rect;
        final Course course;
        CourseHitArea(RectF rect, Course course) { this.rect = rect; this.course = course; }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            if (!scroller.isFinished()) scroller.abortAnimation();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceY) >= Math.abs(distanceX)) {
                scrollYOffset = clamp((int) (scrollYOffset + distanceY), 0, maxScrollY);
                invalidate();
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(velocityY) <= Math.abs(velocityX)) return false;
            scroller.fling(0, scrollYOffset, 0, (int) -velocityY, 0, 0, 0, maxScrollY);
            postInvalidateOnAnimation();
            return true;
        }
    }
}
