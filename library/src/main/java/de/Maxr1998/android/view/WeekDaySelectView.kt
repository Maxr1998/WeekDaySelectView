package de.Maxr1998.android.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.SystemClock
import android.text.TextPaint
import android.util.AttributeSet
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.util.*
import kotlin.math.absoluteValue

class WeekDaySelectView(context: Context, attr: AttributeSet) : View(context, attr) {

    private val daysNum: Int
    private val weeksNum: Int
    private val weekdayNames: Array<String>
    private var pressedState = false
    private val selection = Point(0, 0)
    private var downEventSelection: Point? = null
    private var hoverSelection: Point? = null
    private val textPaint = TextPaint().apply {
        textSize = 14 * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
        color = Color.BLACK
        isAntiAlias = true
    }
    private val textPaintWhite = TextPaint(textPaint).apply { color = Color.WHITE }
    private val circlePaint = Paint().apply { isAntiAlias = true }
    private val hoverCirclePaint = Paint(circlePaint)
    private val defaultCircleRadius = 20 * resources.displayMetrics.density // Hardcoded, will be changeable later
    private var circleRadius = defaultCircleRadius

    private var listener: OnDaySelectListener? = null

    init {
        val viewAttr = context.theme.obtainStyledAttributes(attr, R.styleable.WeekDaySelectView, 0, 0)
        val frameworkAttr = getContext().obtainStyledAttributes(if (SDK_INT >= LOLLIPOP) intArrayOf(android.R.attr.colorPrimary, android.R.attr.colorControlHighlight) else
            intArrayOf(
                    resources.getIdentifier("colorPrimary", "attr", getContext().packageName),
                    resources.getIdentifier("colorControlHighlight", "attr", getContext().packageName)
            )
        )
        try {
            daysNum = viewAttr.getInteger(R.styleable.WeekDaySelectView_numDays, 5)
            weeksNum = viewAttr.getInteger(R.styleable.WeekDaySelectView_numWeeks, 1)
            circlePaint.color = viewAttr.getColor(R.styleable.WeekDaySelectView_selectionColor,
                    frameworkAttr.getColor(0, Color.RED))
            hoverCirclePaint.color = frameworkAttr.getColor(1, 0x7f030060)
        } finally {
            viewAttr.recycle()
            frameworkAttr.recycle()
        }
        isClickable = true
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        weekdayNames = Array(daysNum, {
            calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                    .apply { calendar.add(Calendar.DAY_OF_WEEK, 1) }
        })
    }

    override fun onDraw(canvas: Canvas) {
        val baseX = canvas.width / (2f * daysNum)
        val baseY = canvas.height / (2f * weeksNum)
        canvas.drawCircle(baseX * (2 * selection.x + 1), baseY * (2 * selection.y + 1), circleRadius, circlePaint)
        hoverSelection?.let {
            canvas.drawCircle(baseX * (2 * it.x + 1), baseY * (2 * it.y + 1), circleRadius, hoverCirclePaint)
        }
        val textDeltaY = -(textPaint.ascent() + textPaint.descent()) / 2
        for (y in 0 until weeksNum) {
            for (x in 0 until daysNum) {
                canvas.drawText(weekdayNames[x], baseX * (2 * x + 1), baseY * (2 * y + 1) + textDeltaY, if (isSelected(x, y)) textPaintWhite else textPaint)
            }
        }
        super.onDraw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                if (pressedState && SystemClock.uptimeMillis() - event.downTime < ViewConfiguration.getLongPressTimeout()) {
                    performClick()
                    val newSelection = getSelectionFromEvent(event)
                    if (selection != newSelection) {
                        var dispatched = false
                        ValueAnimator.ofFloat(-defaultCircleRadius, 0f, defaultCircleRadius).apply {
                            duration = 200
                            addUpdateListener { _ ->
                                val value = animatedValue as Float
                                if (!dispatched && value > 0f) {
                                    setSelection(newSelection.x, newSelection.y)
                                    dispatchSelectionChanged()
                                    dispatched = true
                                }
                                circleRadius = value.absoluteValue
                                invalidate()
                            }
                        }.start()
                    }
                }
                pressedState = false
            }
            MotionEvent.ACTION_DOWN -> {
                downEventSelection = getSelectionFromEvent(event)
                pressedState = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (getSelectionFromEvent(event) != downEventSelection) {
                    pressedState = false
                    return false
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedState = false
                return false
            }
        }
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
            hoverSelection = when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> getSelectionFromEvent(event)
                else -> null
            }
            invalidate()
        }
        return super.onGenericMotionEvent(event)
    }

    private fun getSelectionFromEvent(event: MotionEvent) =
            Point(((event.x - x) * daysNum / width).toInt(), ((event.y - y) * weeksNum / height).toInt())

    private fun isSelected(x: Int, y: Int): Boolean =
            x == selection.x && y == selection.y

    private fun dispatchSelectionChanged() {
        listener?.onDaySelected(this, selection)
    }

    fun setSelection(x: Int, y: Int) {
        selection.apply { this.x = x; this.y = y }
        invalidate()
    }

    fun setOnSelectListener(listener: OnDaySelectListener) {
        this.listener = listener
    }

    interface OnDaySelectListener {
        fun onDaySelected(view: WeekDaySelectView, day: Point)
    }
}