package com.lcd.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.lcd.ui.R;

public class DialView extends View
{
    private Paint m_paint;
    private int m_nWidth;//控件宽度
    private int m_nHeight;//控件高度
    private int m_nOuterWidth;//外环直径
    private int m_nInnerWidth;//内环直径
    private int m_nStartAngle;//起始角度
    private int m_nSweepAngle;//扫过的角度
    private float m_fProgress;//进度（当前值/最大值）
    private float m_fMarkTextSize;//刻度字体大小
    private float m_fValueTextSize;//显示值字体大小
    private float m_fUtilTextSize;//单位字体大小
    private float m_fValue;//当前值
    private String m_strUtil;//单位
    private String m_strValueText;//显示值
    private String[] m_strMark;//刻度
    private int[] m_nAraeColor;//颜色区域
    private float[] m_fProportion;//各个颜色区域比例
    private RectF m_rectfOuter;//外环区域
    private RectF m_rectfInner;//内环区域
    private ColorGradientPicker m_colorgradientpick;//渐变着色器
    private int m_nType;//显示类型
    private float m_fAreaInterval;//颜色区域间隔
    private float m_fMaxValue;//最大值
    private int m_nCurrentColor;//当前值
    private float m_fTotalProportion;//颜色区域总比重

    public DialView(Context context)
    {
        super(context);
    }

    public DialView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public DialView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    private void init(Context context, AttributeSet attrs)
    {
        TypedArray typedarray = context.obtainStyledAttributes(attrs, R.styleable.DialView);

        m_fProgress = typedarray.getFloat(R.styleable.DialView_Progress, 0);
        m_fMarkTextSize = typedarray.getFloat(R.styleable.DialView_MarkTextSize, 50);
        m_fValueTextSize = typedarray.getFloat(R.styleable.DialView_ValueTextSize, 50);
        m_fUtilTextSize = typedarray.getFloat(R.styleable.DialView_UtilTextSize, 50);
        m_fAreaInterval = typedarray.getFloat(R.styleable.DialView_AreaInterval, 2);
        m_fValue = typedarray.getFloat(R.styleable.DialView_Value, 0);
        m_fMaxValue = typedarray.getFloat(R.styleable.DialView_MaxValue, 0);
        m_strUtil = typedarray.getString(R.styleable.DialView_Util);

        m_nType = typedarray.getInt(R.styleable.DialView_Type, 0);

        m_nStartAngle = 165;
        m_nSweepAngle = 210;

        int nValueText = typedarray.getResourceId(R.styleable.DialView_ValueText, 0);
        int nMark = typedarray.getResourceId(R.styleable.DialView_Mark, 0);
        int nAreaColor = typedarray.getResourceId(R.styleable.DialView_AraeColor, 0);
        int nProportion = typedarray.getResourceId(R.styleable.DialView_Proportion, 0);

        typedarray.recycle();

        if (nValueText != 0)
        {
            m_strValueText = getResources().getString(nValueText);
        }
        else
        {
            m_strValueText = "";
        }

        //Type0：颜色区域均分， Type1：按比例分割颜色区域
        if (m_nType == 0)
        {
            if (nMark != 0)
            {
                m_strMark = context.getResources().getStringArray(nMark);

                if (m_strMark != null && m_strMark.length > 0)
                {
                    float fValueMax = Float.valueOf(m_strMark[m_strMark.length - 1]);
                    float fValue = m_fValue > fValueMax ? fValueMax : m_fValue;

                    m_fProgress = fValue / Float.valueOf(m_strMark[m_strMark.length - 1]);
                }
            }
        }

        if (m_nType == 1)
        {
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

            if (m_fMaxValue > 0)
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
    }

    private void initGradient(Canvas canvas)
    {
        if (m_paint == null)
            m_paint = new Paint();

        int[] nColors = new int[]{Color.rgb(38,172,236), Color.rgb(207,195,37), Color.rgb(212,83,113)};
        float[] fPosition = new float[]{0.1f, 0.5f, 0.9f};
        LinearGradient linearGradient = new LinearGradient(0,0,m_nWidth, 0, nColors, fPosition, Shader.TileMode.CLAMP);

        m_colorgradientpick = new ColorGradientPicker();
        m_colorgradientpick.setColor(nColors);
        m_colorgradientpick.setPosition(fPosition);

        m_paint.setShader(linearGradient);
    }

    protected void onDraw(Canvas canvas)
    {
        m_rectfOuter = new RectF(m_nOuterWidth * 2, m_nOuterWidth * 2, m_nWidth - m_nOuterWidth * 2, m_nHeight - m_nOuterWidth * 2);

        if (m_nType == 0)
        {
            initGradient(canvas);

            drawOuterCircle(canvas, 150, m_rectfOuter);
            drawOuterArc(canvas, m_rectfOuter);
            drawOuterCircle(canvas, 30, m_rectfOuter);

            float n = (float) (m_nOuterWidth * 3.5);

            m_rectfInner = new RectF(n, n, m_nWidth - n, m_nHeight - n);

            drawInnerArc(canvas, m_rectfInner);
            drawInnerCircle(canvas, - (float)(180 - (m_nSweepAngle * m_fProgress - 30)), m_rectfInner);

            drawText(canvas);
            drawMark(canvas);
        }

        if (m_nType == 1)
        {
            if (m_paint == null)
                m_paint = new Paint();


            drawOuterArc(canvas, m_rectfOuter);

            float n = (float) (m_nOuterWidth * 3.5);

            m_rectfInner = new RectF(n, n, m_nWidth - n, m_nHeight - n);

            drawInnerArc(canvas, m_rectfInner);
            drawInnerCircle(canvas, - (float)(180 - (m_nSweepAngle * m_fProgress - (180 - m_nStartAngle))), m_rectfInner);

            drawText(canvas);
        }
    }

    private void drawOuterArc(Canvas canvas, RectF rectf)
    {
        m_paint.setAntiAlias(true);
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(m_nOuterWidth);

        if (m_nType == 0)
            canvas.drawArc(rectf, m_nStartAngle, m_nSweepAngle, false, m_paint);

        if (m_nType == 1)
        {
            float fAreaAngle = (m_nSweepAngle - ((m_nAraeColor.length - 1) * m_fAreaInterval)) / m_fTotalProportion;

            for (int n = 0; n < m_nAraeColor.length; n++)
            {
                m_paint.setColor(m_nAraeColor[n]);

                float fProportion = 0;

                for (int i = 0; i < n; i++)
                    fProportion +=  m_fProportion[i];

                canvas.drawArc(rectf, m_nStartAngle + fAreaAngle * fProportion + m_fAreaInterval * n, fAreaAngle * m_fProportion[n] , false, m_paint);
            }
        }
    }

    private void drawOuterCircle(Canvas canvas, float nDegree, RectF rectf)
    {
        m_paint.setStrokeWidth(1);
        m_paint.setStyle(Paint.Style.FILL);

        nDegree = (float) (Math.PI / 180f * nDegree);

        canvas.drawCircle((float) (m_nWidth / 2f + rectf.width() / 2 * Math.cos(nDegree)),
                (float) (m_nHeight / 2f + rectf.height() / 2 * Math.sin(nDegree)),
                m_nOuterWidth / 2f,
                m_paint);
    }

    private void drawInnerArc(Canvas canvas, RectF rectf)
    {
        if (m_nType == 0)
        {
            m_nCurrentColor = m_colorgradientpick.getColor(m_fProgress);

            m_paint.setShader(null);
        }

        if (m_nType == 1)
        {
            float fValue = m_fValue > m_fMaxValue ? m_fMaxValue : m_fValue;
            float Proportion = fValue / m_fMaxValue * m_fTotalProportion;
            float fTotal = 0;

            for (int n = 0; n < m_fProportion.length; n++)
            {
                fTotal += m_fProportion[n];

                if (fTotal >= Proportion)
                {
                    m_nCurrentColor = m_nAraeColor[n];
                    break;
                }
            }
        }

        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(m_nInnerWidth);
        m_paint.setColor(m_nCurrentColor);

        canvas.drawArc(rectf, m_nStartAngle, m_nSweepAngle * m_fProgress, false, m_paint);

        m_paint.setColor(Color.rgb(200,200,200));
        canvas.drawArc(rectf, m_nStartAngle + m_nSweepAngle * m_fProgress, (float) (m_nSweepAngle * (1 - m_fProgress)), false, m_paint);
    }

    private void drawInnerCircle(Canvas canvas, float nDegree, RectF rectf)
    {

        m_paint.setColor(m_nCurrentColor);
        m_paint.setStrokeWidth(1);
        m_paint.setStyle(Paint.Style.FILL);

        nDegree = (float) (Math.PI / 180f * nDegree);

        canvas.drawCircle((float) (m_nWidth / 2f + rectf.width() / 2 * Math.cos(nDegree)),
                (float) (m_nHeight / 2f + rectf.height() / 2 * Math.sin(nDegree)),
                (float) (m_nInnerWidth * 1.5),
                m_paint);
    }

    private void drawText(Canvas canvas)
    {
        float fWidthValue = 0;
        float fHeightValue = 0;

        m_paint.setTextSize(m_fValueTextSize);
        m_paint.setColor(m_nCurrentColor);

        if (TextUtils.isEmpty(m_strValueText))
        {
            fWidthValue = getTextWidth(m_fValue + "", m_paint);
            fHeightValue = getTextHeight(m_fValue + "", m_paint);

            canvas.drawText(m_fValue + "", (m_nWidth - fWidthValue) / 2, m_nHeight / 2, m_paint);
        }
        else
        {
            fWidthValue = getTextWidth(m_strValueText, m_paint);
            fHeightValue = getTextHeight(m_strValueText, m_paint);

            canvas.drawText(m_strValueText, (m_nWidth - fWidthValue) / 2, m_nHeight / 2, m_paint);
        }

        if (!TextUtils.isEmpty(m_strUtil) && TextUtils.isEmpty(m_strValueText))
        {
            m_paint.setTextSize(m_fUtilTextSize);
            m_paint.setColor(m_nCurrentColor);

            float fWidthUtil = getTextWidth(m_strUtil, m_paint);

            canvas.drawText(m_strUtil, (m_nWidth - fWidthUtil) / 2, m_nHeight / 2 + fHeightValue + m_nInnerWidth, m_paint);
        }
    }

    private void drawMark(Canvas canvas)
    {
        if (m_strMark != null)
        {
            m_paint.setColor(Color.rgb(200,200,200));
            m_paint.setTextSize(m_fMarkTextSize);

            float f = (float) (Math.PI / 180f * 150);

            float fWidth = (float) (m_nWidth / 2f + m_rectfOuter.width() / 2 * Math.cos(f));
            float fHeight = (float) (m_nHeight / 2f + m_rectfOuter.height() / 2 * Math.sin(f));

            canvas.drawText(m_strMark[0], (float) (fWidth - m_nOuterWidth * 1.5), fHeight,m_paint );
            canvas.drawText(m_strMark[6], (float) (fWidth  - m_nOuterWidth * 4.5 + m_nWidth), fHeight,m_paint );

            f = (float) (Math.PI / 180f * 190);
            fWidth = (float) (m_nWidth / 2f + m_rectfOuter.width() / 2 * Math.cos(f));
            fHeight = (float) (m_nHeight / 2f + m_rectfOuter.height() / 2 * Math.sin(f));

            canvas.drawText(m_strMark[1], (float) (fWidth - m_nOuterWidth * 1.3), fHeight,m_paint );
            canvas.drawText(m_strMark[5], (float) (fWidth - m_nOuterWidth * 3.3 + m_nWidth), fHeight,m_paint );

            f = (float) (Math.PI / 180f * 230);
            fWidth = (float) (m_nWidth / 2f + m_rectfOuter.width() / 2 * Math.cos(f));
            fHeight = (float) (m_nHeight / 2f + m_rectfOuter.height() / 2 * Math.sin(f));

            canvas.drawText(m_strMark[2], (float) (fWidth - m_nOuterWidth * 1.5), fHeight,m_paint );
            canvas.drawText(m_strMark[4], (float) (fWidth  - m_nOuterWidth * 7 + m_nWidth), fHeight,m_paint );

            fWidth = getTextWidth(m_strMark[3], m_paint);

            canvas.drawText(m_strMark[3], (m_nWidth - fWidth) / 2, m_nOuterWidth, m_paint );
        }
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int nMode = MeasureSpec.getMode(widthMeasureSpec);
        int nWidth = MeasureSpec.getSize(widthMeasureSpec);
        int nHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (nMode == MeasureSpec.AT_MOST)
        {
            nWidth = 100;
            nHeight = 100;
        }

        if (nMode == MeasureSpec.EXACTLY)
        {
            if (nWidth < nHeight)
            {
                nHeight = nWidth;
            } else
                nWidth = nHeight;
        }

        m_nWidth = nWidth;
        m_nHeight = nHeight;

        m_nOuterWidth = m_nWidth / 15;

        m_nInnerWidth = m_nWidth / 60;

        setMeasuredDimension(m_nWidth, m_nHeight / 4 * 3);
    }

    private void resetProportion(){
        m_fTotalProportion = 0;

        for (float fProportion: m_fProportion)
            m_fTotalProportion += fProportion;
    }

    private void resetProgress()
    {
        if (m_nType == 0)
        {
            if (m_strMark != null && m_strMark.length > 0)
            {
                float fValueMax = Float.valueOf(m_strMark[m_strMark.length - 1]);
                float fValue = m_fValue > fValueMax ? fValueMax : m_fValue;

                m_fProgress = fValue / Float.valueOf(m_strMark[m_strMark.length - 1]);
            }
        }

        if (m_nType == 1)
        {
            float fValue = m_fValue > m_fMaxValue ? m_fMaxValue : m_fValue;

            m_fProgress = fValue / m_fMaxValue;
        }
    }

    public void setProgress(float fProcess)
    {
        if (fProcess >= 0 && fProcess <= 1)
            m_fProgress = fProcess;

        invalidate();
    }

    public void setMark(String[] strMark)
    {
        if (strMark != null && strMark.length > 0)
        {
            m_strMark = strMark;

            resetProgress();
            invalidate();
        }
    }

    public void setValue(float fValue)
    {
        if (fValue >= 0)
        {
            m_fValue = fValue;

            resetProgress();
            invalidate();
        }
    }

    public void setMaxValue(float fmaxValue)
    {
        m_fMaxValue = fmaxValue;

        resetProgress();

        invalidate();
    }

    public void setValueText(String strValueText)
    {
        m_strValueText = strValueText;
        invalidate();
    }

    public void setProportion(float[] fProportion){
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
                        m_fProportion[i] = Float.valueOf("1");
                }
            }

            resetProportion();

            invalidate();
        }
    }

    public float[] getProportion(){
        return m_fProportion;
    }

    public void setAraeColor(int[] nAraeColor){
        if (nAraeColor != null){
            m_nAraeColor = nAraeColor;
            invalidate();
        }
    }

    public int[] getColorArea(){
        return m_nAraeColor;
    }

    public int getCurrentColor(){
        if (m_nType == 0)
            m_nCurrentColor = m_colorgradientpick.getColor(m_fProgress);

        if (m_nType == 1)
        {
            float Proportion = m_fValue / m_fMaxValue * m_fTotalProportion;
            float fTotal = 0;

            for (int n = 0; n < m_fProportion.length; n++)
            {
                fTotal += m_fProportion[n];

                if (fTotal >= Proportion)
                {
                    m_nCurrentColor = m_nAraeColor[n];
                    break;
                }
            }
        }

        return m_nCurrentColor;
    }

    public float getValue(){
        return m_fValue;
    }

    public float getMaxValue(){
        return m_fMaxValue;
    }

    public float getProgress(){
        return m_fProgress;
    }

    private class ColorGradientPicker
    {
        private int[] mColorArr;
        private float[] mColorPosition;

        public void setColor(int[] nColor)
        {
            mColorArr = nColor;
        }

        public void setPosition(float[] fPosition)
        {
            mColorPosition = fPosition;
        }

        public int getColor(float radio)
        {
            int startColor;
            int endColor;

            if (radio >= 1)
                return mColorArr[mColorArr.length - 1];

            for (int i = 0; i < mColorPosition.length; i++)
            {
                if (radio <= mColorPosition[i])
                {
                    if (i == 0)
                        return mColorArr[0];

                    startColor = mColorArr[i - 1];
                    endColor = mColorArr[i];

                    float areaRadio = getAreaRadio(radio,mColorPosition[i-1],mColorPosition[i]);

                    return getColorFrom(startColor,endColor,areaRadio);
                }
            }

            return -1;
        }

        public float getAreaRadio(float radio, float startPosition, float endPosition)
        {
            return (radio - startPosition) / (endPosition - startPosition);
        }

        public int getColorFrom(int startColor, int endColor, float radio)
        {
            int redStart = Color.red(startColor);
            int blueStart = Color.blue(startColor);
            int greenStart = Color.green(startColor);
            int redEnd = Color.red(endColor);
            int blueEnd = Color.blue(endColor);
            int greenEnd = Color.green(endColor);

            int red = (int) (redStart + ((redEnd - redStart) * radio + 0.5));
            int greed = (int) (greenStart + ((greenEnd - greenStart) * radio + 0.5));
            int blue = (int) (blueStart + ((blueEnd - blueStart) * radio + 0.5));
            return Color.argb(255, red, greed, blue);
        }
    }
}