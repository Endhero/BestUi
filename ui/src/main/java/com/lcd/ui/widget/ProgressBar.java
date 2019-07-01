package com.lcd.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.lcd.ui.R;

public class ProgressBar extends View {
    private String[] m_strMark;//刻度
    private int[] m_nAraeColor;//颜色区域
    private int m_nClearAreaColor;//清空区域颜色
    private float[] m_fProportion;//各个颜色区域比例
    private float m_fStrokeProportion;//边框比例
    private float m_fTotalProportion;//总比例
    private float m_fProgress;//进度（当前值/最大值）
    private float m_fMarkTextSize;//刻度字体大小
    private int m_nMarkTextColor;//刻度颜色
    private Paint m_paint;
    private int m_nWidth;//控件宽度
    private int m_nHeight;//控件高度
    private float m_fMarkHeight;//刻度高度
    private int m_nProgressBarHeight;//进度条高度
    private int m_nIntervalHeight;//刻度和进度条间隔
    private String m_strMarkStart;//起始刻度
    private float m_fValue;//当前值
    private float m_fMaxValue;//最大值

    public ProgressBar(Context context) {
        super(context);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs){
        m_paint = new Paint();
        m_paint.setAntiAlias(true);

        TypedArray typedarray = context.obtainStyledAttributes(attrs, R.styleable.ProgressBar);

        m_fProgress = typedarray.getFloat(R.styleable.ProgressBar_Progress, 0);
        m_fMarkTextSize = typedarray.getFloat(R.styleable.ProgressBar_MarkTextSize, 50);
        m_fValue = typedarray.getFloat(R.styleable.ProgressBar_Value, 0);
        m_fMaxValue = typedarray.getFloat(R.styleable.ProgressBar_MaxValue, 100);
        m_fStrokeProportion = typedarray.getFloat(R.styleable.ProgressBar_StrokeProportion, (float) 0.125);

        m_strMarkStart = typedarray.getString(R.styleable.ProgressBar_MarkStart);

        m_nMarkTextColor = typedarray.getInt(R.styleable.ProgressBar_MarkTextColor, Color.rgb(0,0,0));
        m_nClearAreaColor = typedarray.getInt(R.styleable.ProgressBar_ClearAreaColor, Color.rgb(255, 255, 255));

        int nMark = typedarray.getResourceId(R.styleable.ProgressBar_Mark, 0);
        int nAreaColor = typedarray.getResourceId(R.styleable.ProgressBar_AraeColor, 0);
        int nProportion = typedarray.getResourceId(R.styleable.ProgressBar_Proportion, 0);

        if  (m_fValue > m_fMaxValue)
            m_fValue = m_fMaxValue;

        if (m_strMarkStart == null)
            m_strMarkStart = "";

        if (m_fStrokeProportion < 0)
            m_fStrokeProportion = 0;

        if (m_fStrokeProportion > 0.5)
            m_fStrokeProportion = (float) 0.5;

        if (nMark != 0)
        {
            m_strMark = context.getResources().getStringArray(nMark);
        }
        else
            m_strMark = new String[]{""};

        if (nAreaColor != 0)
        {
            m_nAraeColor = context.getResources().getIntArray(nAreaColor);
        }
        else
            m_nAraeColor = new int []{
                    Color.rgb(10,169,239),
                    Color.rgb(55,194,150),
                    Color.rgb(146,181,26),
                    Color.rgb(230,190,0),
                    Color.rgb(255,151,0),
                    Color.rgb(240,105,105),
                    Color.rgb(188,59,88)};

        if (m_fMaxValue != 0)
        {
            float fValue = m_fValue > m_fMaxValue ? m_fMaxValue : m_fValue;

            m_fProgress = fValue / m_fMaxValue;
        }

        if (nProportion != 0)
        {
            String[] strProportion = context.getResources().getStringArray(nProportion);

            m_fProportion = new float[m_nAraeColor.length];

            if (strProportion.length >= m_nAraeColor.length)
            {
                for (int i = 0; i < m_nAraeColor.length; i++)
                    m_fProportion[i] = Float.valueOf(strProportion[i]);
            }
            else
            {
                for (int i = 0; i < m_nAraeColor.length; i++)
                {
                    if (i < strProportion.length)
                    {
                        m_fProportion[i] = Float.valueOf(strProportion[i]);
                    }
                    else
                        m_fProportion[i] = 1;
                }
            }
        }
        else
        {
            m_fProportion = new float[m_nAraeColor.length];

            for (int i = 0; i < m_fProportion.length; i++)
                m_fProportion[i] = 1;
        }

        m_fTotalProportion = 0;

        for (float fProportion: m_fProportion)
            m_fTotalProportion += fProportion;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawMark(canvas);
        drawProgressBar(canvas);
        drawClearArea(canvas);
    }

    private void drawProgressBar(Canvas canvas)
    {
        int nCircleRadius = m_nProgressBarHeight / 2;
        int nY = (int) (m_fMarkHeight + m_nIntervalHeight);

        if (m_fProportion != null && m_fProportion.length > 1)
        {
            float nWidthPerProportion = m_nWidth / m_fTotalProportion;

            float fX = nCircleRadius;

            for (int i = 0; i < m_fProportion.length; i++)
            {
                int nOffset = 0;

                if (i == 0 || i== m_fProportion.length - 1)
                    nOffset = nCircleRadius;

                float fWidth = nWidthPerProportion * m_fProportion[i] - nOffset;

                RectF rectf = new RectF(fX, nY, fX + fWidth,nY + nCircleRadius * 2);
                fX += fWidth;

                m_paint.setColor(m_nAraeColor[i]);

                canvas.drawRect(rectf,m_paint);
            }
        }

        RectF rectf = new RectF(0, nY, nCircleRadius * 2,nY + nCircleRadius * 2);

        m_paint.setColor(m_nAraeColor[0]);
        canvas.drawArc(rectf, 90, 180, false, m_paint);

        int nX = m_nWidth - nCircleRadius * 2;

        rectf = new RectF(nX, nY, nX + nCircleRadius * 2,nY + nCircleRadius * 2);

        m_paint.setColor(m_nAraeColor[m_nAraeColor.length - 1]);
        canvas.drawArc(rectf, 180, 270, false, m_paint);
    }

    private void drawClearArea(Canvas canvas)
    {
        float fInnerStrokeWidth = m_nProgressBarHeight * m_fStrokeProportion;
        float fInnerCircleRadius = m_nProgressBarHeight / 2 - fInnerStrokeWidth;
        float fY = m_fMarkHeight + m_nIntervalHeight + fInnerStrokeWidth;
        float fX = fInnerStrokeWidth + (m_nWidth - fInnerStrokeWidth * 2) * m_fProgress;
        float fShowAreaWidth =  (m_nWidth - fInnerStrokeWidth * 2) * m_fProgress;
        float fClearAreaWidth = (m_nWidth - fInnerStrokeWidth * 2) * (1 - m_fProgress);

        if (fShowAreaWidth < fInnerCircleRadius)
        {
            RectF rectf = new RectF(fInnerStrokeWidth , fY, fInnerStrokeWidth + fInnerCircleRadius * 2, fY + fInnerCircleRadius * 2);

            float fRatio = fShowAreaWidth / fInnerCircleRadius;
            float fAngle = 360 * fRatio;

            m_paint.setColor(m_nClearAreaColor);
            canvas.drawArc(rectf,180 + fAngle / 2, 360 - fAngle,false, m_paint);

            m_paint.setColor(m_nClearAreaColor);

            rectf = new RectF(fInnerStrokeWidth + fInnerCircleRadius, fY, m_nWidth - fInnerStrokeWidth - fInnerCircleRadius, fY + fInnerCircleRadius * 2);
            canvas.drawRect(rectf, m_paint);

            m_paint.setColor(m_nClearAreaColor);
            rectf = new RectF(m_nWidth - fInnerStrokeWidth - fInnerCircleRadius * 2, fY, m_nWidth - fInnerStrokeWidth, fY + fInnerCircleRadius * 2);

            canvas.drawArc(rectf, 270, 180, false, m_paint);
        }else if (fClearAreaWidth < fInnerCircleRadius)
        {
            RectF rectf = new RectF(m_nWidth - fInnerStrokeWidth - fInnerCircleRadius * 2 , fY, m_nWidth - fInnerStrokeWidth, fY + fInnerCircleRadius * 2);

            m_paint.setColor(m_nClearAreaColor);

            float fRatio = fClearAreaWidth / fInnerCircleRadius;
            float fAngle = 180 * fRatio;

            canvas.drawArc(rectf,360 - fAngle / 2, fAngle,false, m_paint);
        }
        else
        {
            RectF rectf = new RectF(fX, fY, fX + fClearAreaWidth - fInnerCircleRadius, fY + fInnerCircleRadius * 2);

            m_paint.setColor(m_nClearAreaColor);
            canvas.drawRect(rectf, m_paint);

            rectf = new RectF(fX + fClearAreaWidth - fInnerCircleRadius * 2, fY, fX + fClearAreaWidth, fY + fInnerCircleRadius * 2);

            canvas.drawArc(rectf, 270, 180 ,false, m_paint);
        }
    }

    public void drawMark(Canvas canvas)
    {
        if (m_fTotalProportion == 0)
        {
            for (float fProportion : m_fProportion)
            {
                m_fTotalProportion += fProportion;
            }
        }

        m_paint.setColor(m_nMarkTextColor);
        m_paint.setTextSize(m_fMarkTextSize);

        float fProportion = 0;

        for (int i = 0; i < m_strMark.length; i++)
        {
            fProportion += m_fProportion[i];

            String str = m_strMark[i];
            float fTextWidth = getTextWidth(str, m_paint);
            float fTextViewHeight = getTextHeight(str, m_paint);

            float fOffset = fTextWidth / 2;

            if (i == m_strMark.length - 1)
                fOffset = fTextWidth;

            canvas.drawText(str, m_nWidth * fProportion / m_fTotalProportion - fOffset, fTextViewHeight, m_paint);
        }

        canvas.drawText(m_strMarkStart, 0, getTextHeight(m_strMarkStart, m_paint), m_paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int nModeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int nModeHeight = MeasureSpec.getMode(heightMeasureSpec);
        int nWidth = MeasureSpec.getSize(widthMeasureSpec);
        int nHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (nModeWidth == MeasureSpec.AT_MOST)
        {
            nWidth = 100;
        }

        if (nModeHeight == MeasureSpec.AT_MOST)
        {
            nHeight = 100;
        }

        m_nWidth = nWidth;
        m_nHeight = nHeight;

        if (m_strMark != null && m_strMark.length > 0)
        {
            m_paint.setTextSize(m_fMarkTextSize);
            m_fMarkHeight = getTextHeight(m_strMark[0], m_paint);

            m_nIntervalHeight = (int) ((m_nHeight - m_fMarkHeight) / 5);
        }

        m_nProgressBarHeight = (int) (m_nHeight - m_fMarkHeight - m_nIntervalHeight);

        setMeasuredDimension(m_nWidth, m_nHeight);
    }

    private float getTextWidth(String str, Paint paint)
    {
        return paint.measureText(str,0,str.length());
    }

    private float getTextHeight(String str, Paint paint)
    {
        Rect rect = new Rect();
        paint.getTextBounds(str,0,str.length(), rect);

        return rect.height();
    }

    public void setProgress(float fProcess)
    {
        if (fProcess >= 0 && fProcess <= 1)
            m_fProgress = fProcess;

        invalidate();
    }

    public void setValue(float fValue)
    {
        if (fValue < 0)
            fValue = 0;

        if  (fValue > m_fMaxValue)
            fValue = m_fMaxValue;

        m_fValue = fValue;

        resetProgress();
    }

    public void setMaxValue(float fMaxValue)
    {
        m_fMaxValue = fMaxValue;

        resetProgress();
    }

    public float getMaxValue()
    {
        return m_fMaxValue;
    }

    public void setMarkStart(String strMarkStart)
    {
        m_strMarkStart = strMarkStart;
    }

    public void setAraeColor(int[] nAraeColor){
        if (nAraeColor != null){
            m_nAraeColor = nAraeColor;

            invalidate();
        }
    }

    public void setStrokeProportion(float fProportion)
    {
        if (fProportion < 0)
            fProportion = 0;

        if (fProportion > 0.5)
            fProportion = (float) 0.5;

        m_fStrokeProportion = fProportion;

        invalidate();
    }

    public void setProportion(float[] fProportion)
    {
        if (fProportion != null){
            m_fProportion = new float[m_nAraeColor.length];

            if (fProportion.length >= m_nAraeColor.length)
            {
                for (int i = 0; i < m_nAraeColor.length; i++)
                    m_fProportion[i] = fProportion[i];
            }
            else
            {
                for (int i = 0; i < m_nAraeColor.length; i++)
                {
                    if (i < fProportion.length)
                    {
                        m_fProportion[i] = fProportion[i];
                    }
                    else
                        m_fProportion[i] = 1;
                }
            }

            resetProportion();

            invalidate();
        }
    }

    public float[] getProportion()
    {
        return m_fProportion;
    }

    public float getTotalProportion()
    {
        return m_fTotalProportion;
    }

    private void resetProgress()
    {
        m_fProgress = m_fValue / m_fMaxValue;

        invalidate();
    }

    public int getCurrentColor()
    {
        float fProportion = 0;

        for (int i = 0; i < m_fProportion.length; i++)
        {
            fProportion += m_fProportion[i];

            if (fProportion >= m_fProgress * m_fTotalProportion )
                return m_nAraeColor[i];
        }

        return 0;
    }

    public void setMark(String[] srtMark){
        m_strMark = srtMark;
    }

    public String[] getMark()
    {
        return m_strMark;
    }

    public void setValueReal(float fValueReal)
    {
        float fTotalProportion = getTotalProportion();
        float fMaxValue = getMaxValue();
        float[] fProportion = getProportion();
        String[] strMark = getMark();
        float fProportionValue = 0;
        float fValue = 0;

        float[] fMark = new float[strMark.length];

        if (fValueReal <= fMaxValue){
            for (int i = 0; i < fMark.length; i++){
                if (!TextUtils.isEmpty(strMark[i])){
                    fMark[i] = Float.valueOf(strMark[i]);
                }else {
                    fMark[i] = fMaxValue;
                }
            }

            for (int i = 0; i < fMark.length; i++){
                if (fValueReal <= fMark[i]){
                    float fMarkValue = 0;

                    if (i > 0){
                        if (i == strMark.length - 1){
                            fMarkValue = fMaxValue - Float.valueOf(strMark[i - 1]);
                        }else{
                            fMarkValue = Float.valueOf(strMark[i]) - Float.valueOf(strMark[i - 1]);
                        }

                        fValue = ((fValueReal - fMark[i - 1]) / fMarkValue * fProportion[i] + fProportionValue) * fMaxValue / fTotalProportion;

                    }else{
                        fMarkValue = Float.valueOf(strMark[0]);
                        fValue = (fValueReal / fMarkValue * fProportion[i] + fProportionValue) * fMaxValue / fTotalProportion;
                    }

                    break;
                }

                fProportionValue += fProportion[i];
            }
        }else {
            fValue = fMaxValue;
        }

        setValue(fValue);
    }

    private void resetProportion(){
        m_fTotalProportion = 0;

        for (float fProportion: m_fProportion)
            m_fTotalProportion += fProportion;
    }
}
