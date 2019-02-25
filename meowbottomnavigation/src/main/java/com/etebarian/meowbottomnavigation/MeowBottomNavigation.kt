@file:Suppress("unused")

package com.etebarian.meowbottomnavigation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Created by 1HE on 10/23/2018.
 */

internal typealias IBottomNavigationListener = (model: MeowBottomNavigation.Model) -> Unit

@Suppress("MemberVisibilityCanBePrivate")
class MeowBottomNavigation : FrameLayout {//todo test orientation & slow anim

    private var models = ArrayList<Model>()
    private var cells = ArrayList<MeowBottomNavigationCell>()

    private var nowShowId = -1

    private var mOnClickedListener: IBottomNavigationListener = {}
    private var mOnShowListener: IBottomNavigationListener = {}

    var heightCell = 0

    private var defaultIconColor = Color.parseColor("#757575")
    private var selectedIconColor = Color.parseColor("#2196f3")
    private var backgroundBottomColor = Color.parseColor("#ffffff")
    private var countTextColor = Color.parseColor("#ffffff")
    private var countBackgroundColor = Color.parseColor("#ff0000")
    private var countTypeface: Typeface? = null
    private var rippleColor = Color.parseColor("#757575")

    private lateinit var ll_cells: LinearLayout
    private lateinit var bezierView: BezierView

    init {
        heightCell = dip(context, 68)
    }

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setAttributeFromXml(context, attrs)
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setAttributeFromXml(context, attrs)
        initializeViews()
    }

    private fun setAttributeFromXml(context: Context, attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.MeowBottomNavigation, 0, 0)
        try {
            a?.apply {
                defaultIconColor = getColor(R.styleable.MeowBottomNavigation_mbn_defaultIconColor, defaultIconColor)
                selectedIconColor = getColor(R.styleable.MeowBottomNavigation_mbn_selectedIconColor, selectedIconColor)
                backgroundBottomColor = getColor(R.styleable.MeowBottomNavigation_mbn_backgroundBottomColor, backgroundBottomColor)
                countTextColor = getColor(R.styleable.MeowBottomNavigation_mbn_countTextColor, countTextColor)
                countBackgroundColor = getColor(R.styleable.MeowBottomNavigation_mbn_countBackgroundColor, countBackgroundColor)
                val typeface = getString(R.styleable.MeowBottomNavigation_mbn_countTypeface)
                rippleColor = getColor(R.styleable.MeowBottomNavigation_mbn_rippleColor, rippleColor)

                if (typeface != null)
                    countTypeface = Typeface.createFromAsset(context.assets, typeface)
            }
        } finally {
            a?.recycle()
        }
    }

    private fun initializeViews() {
        ll_cells = LinearLayout(context)
        ll_cells.apply {
            val params = FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightCell)
            params.gravity = Gravity.BOTTOM
            layoutParams = params
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }

        bezierView = BezierView(context)
        bezierView.apply {
            layoutParams = FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightCell)
            color = backgroundBottomColor
        }

        addView(bezierView)
        addView(ll_cells)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (nowShowId == -1 && models.isNotEmpty()) {
            show(models.first().id, false)
        }
    }

    fun add(model: Model) {
        val cell = MeowBottomNavigationCell(context)
        cell.apply {
            val params = LinearLayout.LayoutParams(0, heightCell, 1f)
            layoutParams = params
            icon = model.icon
            count = model.count
            circleColor = this@MeowBottomNavigation.backgroundBottomColor
            countTextColor = this@MeowBottomNavigation.countTextColor
            countBackgroundColor = this@MeowBottomNavigation.countBackgroundColor
            countTypeface = this@MeowBottomNavigation.countTypeface
            rippleColor = this@MeowBottomNavigation.rippleColor
            defaultIconColor = this@MeowBottomNavigation.defaultIconColor
            selectedIconColor = this@MeowBottomNavigation.selectedIconColor
            setOnClickListener {
                show(model.id)
                mOnClickedListener(model)
            }
            disableCell()
            ll_cells.addView(this)
        }

        cells.add(cell)
        models.add(model)
    }

    fun show(id: Int, enableAnimation: Boolean = true) {
        for (i in models.indices) {
            val model = models[i]
            val cell = cells[i]
            if (model.id == id) {
                anim(cell, id, enableAnimation)
                cell.enableCell()
                mOnShowListener(model)
            } else {
                cell.disableCell()
            }
        }
        nowShowId = id
    }

    private fun anim(cell: MeowBottomNavigationCell, id: Int, enableAnimation: Boolean = true) {
        val animDuration = if (enableAnimation) 750L else 1L
        val animInterpolator = FastOutSlowInInterpolator()

        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.apply {
            duration = animDuration
            interpolator = animInterpolator
            val beforeX = bezierView.bezierX
            addUpdateListener {
                val f = it.animatedFraction
                val newX = cell.x + (cell.measuredWidth / 2)
                if (newX > beforeX)
                    bezierView.bezierX = f * (newX - beforeX) + beforeX
                else
                    bezierView.bezierX = beforeX - f * (beforeX - newX)
            }
            start()
        }

        val pos = getModelPosition(id)
        val nowPos = getModelPosition(nowShowId)
        if (Math.abs(pos - nowPos) > 1) {
            val progressAnim = ValueAnimator.ofFloat(0f, 1f)
            progressAnim.apply {
                duration = animDuration
                interpolator = animInterpolator
                addUpdateListener {
                    val f = it.animatedFraction
                    bezierView.progress = f * 2f
                }
                start()
            }
        }

        cell.isFromLeft = pos > nowPos
    }

    fun isShowing(id: Int): Boolean {
        return nowShowId == id
    }

    fun getModelById(id: Int): Model? {
        models.forEach {
            if (it.id == id)
                return it
        }
        return null
    }

    fun getCellById(id: Int): MeowBottomNavigationCell? {
        return cells[getModelPosition(id)]
    }

    fun getModelPosition(id: Int): Int {
        for (i in models.indices) {
            val item = models[i]
            if (item.id == id)
                return i
        }
        return -1
    }

    fun setCount(id: Int, count: String) {
        val model = getModelById(id) ?: return
        val pos = getModelPosition(id)
        model.count = count
        cells[pos].count = count
    }

    fun setOnShowListener(listener: IBottomNavigationListener) {
        mOnShowListener = listener
    }

    fun setOnClickMenuListener(listener: IBottomNavigationListener) {
        mOnClickedListener = listener
    }

    class Model(var id: Int, var icon: Int) {

        var count: String = MeowBottomNavigationCell.EMPTY_VALUE

    }
}