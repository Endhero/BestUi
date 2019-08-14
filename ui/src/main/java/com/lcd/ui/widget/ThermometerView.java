package com.lcd.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.lcd.ui.R;

public class ThermometerView extends View
{
    private int m_nWidth;
    private int m_nHeight;
    private int m_nInnerWidth;
    private int m_nInnerHeight;
    private Paint m_paint;
    private String[] m_strMark;
    private float m_fMarkTextSize;
    private float m_fValue;
    private int m_nLiquidColor;
    private int m_nShellThick;
    private int m_nMarkLineThick;
    private int m_nShellColor;
    private int m_nMarkLineColor;
    private int m_nMarkTextColor;

    public ThermometerView(Context context)
    {
        super(context);
    }

    public ThermometerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public ThermometerView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        drawShell(canvas);
        drawMarkLines(canvas);
        drawMarkText(canvas);
        drawGlassBubble(canvas);
        drawGlassTuBe(canvas);
    }

    private void drawShell(Canvas canvas)
    {
        m_paint.setColor(m_nShellColor);
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(m_nShellThick);

        int nLeft = (m_nWidth - m_nInnerWidth) / 2;
        int nTop = m_nHeight / 10;
        int nRight = (m_nWidth + m_nInnerWidth) / 2;
        int nBottom = m_nHeight / 10 * 9;

        RectF rectf = new RectF(nLeft,nTop,nRight, nBottom);

        canvas.drawRoundRect(rectf, m_nInnerWidth / 2, m_nInnerWidth/ 2, m_paint);
    }

    private void drawMarkLines(Canvas canvas)
    {
        m_paint.setColor(m_nMarkLineColor);
        m_paint.setStyle(Paint.Style.FILL);
        m_paint.setStrokeWidth(m_nMarkLineThick);

        int nSize = m_strMark.length - 1;//-1是为了均分刻度范围，让刻度线与温度计上下端保持等距
        int nHeight = m_nInnerHeight / 5 * 3;
        int nInterval =  nHeight / nSize;

        int nXStart = (m_nWidth - m_nInnerWidth) / 2;
        int nYStart = 0;
        int nXStop = nXStart + m_nInnerWidth / 4;
        int nYStop = 0;

        for (int n = 0; n < m_strMark.length; n++)
        {
            nYStart = (m_nHeight + nHeight) / 2 - nInterval * n;
            nYStop = nYStart;

            canvas.drawLine(nXStart, nYStart, nXStop, nYStop, m_paint);
        }
    }

    private void drawMarkText(Canvas canvas)
    {
        m_paint.setColor(m_nMarkTextColor);
        m_paint.setStyle(Paint.Style.FILL);
        m_paint.setStrokeWidth(1);
        m_paint.setTextSize(m_fMarkTextSize);

        int nSize = m_strMark.length - 1;
        int nHeight = m_nInnerHeight / 5 * 3;
        int nInterval =  nHeight / nSize;

        int nXStart = (m_nWidth - m_nInnerWidth) / 2;
        int nYStart = 0;

        for (int n = 0; n < m_strMark.length; n++)
        {
            nYStart = (m_nHeight + nHeight) / 2 - nInterval * n;
            canvas.drawText(m_strMark[n], nXStart - getTextWidth(m_strMark[n], m_paint) - m_nInnerWidth / 3, nYStart, m_paint);
        }
    }

    private void drawGlassBubble(Canvas canvas)
    {
        m_paint.setColor(m_nLiquidColor);
        m_paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(m_nWidth / 2, m_nHeight / 10 * 9 - m_nInnerWidth / 2,m_nInnerWidth / 3 ,m_paint);
    }

    private void drawGlassTuBe(Canvas canvas)
    {
        if (m_strMark != null && m_strMark.length > 0)
        {
            float fMin = Float.valueOf(m_strMark[0]);
            float fMax = Float.valueOf(m_strMark[m_strMark.length - 1]);

            if (m_fValue >= fMin && m_fValue <= fMax)
            {
                m_paint.setColor(m_nLiquidColor);
                m_paint.setStyle(Paint.Style.FILL);

                float fLeft = m_nWidth / 2 - m_nInnerWidth / 10;
                float fTop = m_nHeight / 10 * 9 - m_nInnerHeight / 5 - (m_fValue - fMin) / (fMax - fMin) * m_nInnerHeight / 5 * 3;
                float fRight = m_nWidth / 2 + m_nInnerWidth / 10;
                float fBottom = m_nHeight / 10 * 9 - m_nInnerWidth / 2;

                RectF rectf = new RectF(fLeft, fTop, fRight, fBottom);

                canvas.drawRoundRect(rectf, m_nInnerWidth / 5, m_nInnerWidth / 5, m_paint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int nModeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int nModeHeight = MeasureSpec.getMode(widthMeasureSpec);
        int nWidth = MeasureSpec.getSize(widthMeasureSpec);
        int nHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (nModeWidth == MeasureSpec.AT_MOST)
            nWidth = 100;

        if (nModeHeight == MeasureSpec.AT_MOST)
            nHeight = 100;

        m_nWidth = nWidth;
        m_nHeight = nHeight;
        m_nInnerHeight = m_nHeight /5 * 4;
        m_nInnerWidth = m_nWidth / 3;

        setMeasuredDimension(m_nWidth, m_nHeight);
    }

    private void init(Context context,AttributeSet attrs)
    {
        m_paint = new Paint();
        m_paint.setAntiAlias(true);

        TypedArray typedarray = context.obtainStyledAttributes(attrs, R.styleable.ThermometerView);

        m_fMarkTextSize = typedarray.getFloat(R.styleable.ThermometerView_MarkTextSize, 50);
        m_fValue = typedarray.getFloat(R.styleable.ThermometerView_Value, 35);
        m_nLiquidColor = typedarray.getInteger(R.styleable.ThermometerView_LiquidColor, Color.RED);
        m_nShellThick = typedarray.getInteger(R.styleable.ThermometerView_ShellThick, 5);
        m_nMarkLineThick = typedarray.getInteger(R.styleable.ThermometerView_MarkLineThick, 5);
        m_nMarkTextColor = typedarray.getInteger(R.styleable.ThermometerView_MarkLineColor, Color.rgb(153,153,153));
        m_nShellColor = typedarray.getInteger(R.styleable.ThermometerView_ShellColor, Color.rgb(34,132,191));
        m_nMarkLineColor = typedarray.getInteger(R.styleable.ThermometerView_MarkLineColor, Color.rgb(34,132,191));

        int n = typedarray.getResourceId(R.styleable.ThermometerView_Mark, 0);

        typedarray.recycle();

        if (n != 0)
            m_strMark = context.getResources().getStringArray(n);

        else
            m_strMark = new String[]{"35", "36", "37", "38", "39", "40", "41", "42"};
    }

    public void setMarks(String[] strMarks)
    {
        if (strMarks != null && strMarks.length > 0)
        {
            m_strMark = strMarks;
            invalidate();
        }

    }

    public void setTextSize(float f)
    {
        m_fMarkTextSize = f;
        invalidate();
    }

    public void setValue(float f)
    {
        float fMax = Float.valueOf(m_strMark[m_strMark.length - 1]);

        if (f > fMax)
            f = fMax;

        m_fValue = f;

        invalidate();
    }

    public float getValue()
    {
        return m_fValue;
    }

    public void setLiquidColor(int nColor)
    {
        m_nLiquidColor = nColor;
        invalidate();
    }

    public void setShellThick(int nThick)
    {
        m_nShellThick = nThick;
        invalidate();
    }

    public void setMarkLineThick(int nThick)
    {
        m_nMarkLineThick = nThick;
        invalidate();
    }

    public void setShellColor(int nColor)
    {
        m_nShellColor = nColor;
        invalidate();
    }

    public void setMarkLineColor(int nColor)
    {
        m_nMarkLineColor = nColor;
    }

    public void setMarkTextColor(int nColor)
    {
        m_nMarkTextColor = nColor;
        invalidate();
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
}
