package com.example.krypto2factor.Utils;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.example.krypto2factor.R;

/**
 * Class to deliver a smooth ring-form progress bar
 */
public class CircularProgressBar extends View {
    // Line-thickness
    private float lineThickness = 4;
    // To keep track of progress
    private float progress = 0;
    // min/max Values
    private int min = 0;
    private int max = 100;
    // determine starting point -> 12 o' clock
    private int initialAngle = -90;
    // background color
    private int bgColor = Color.DKGRAY;
    private int fgColor = Color.BLUE;
    private float bgTransparency = 0.3f;
    // graphics and colors
    private RectF rectF;
    private Paint pBackground;
    private Paint pForeground;

    /**
     * Override Method to determine the measurements of our view to determine the size of the circular ProgressBar
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int min = Math.min(width, height);
        // Inform system about view dimensions
        setMeasuredDimension(min, min);
        // Adjust rect size to view dimensions
        rectF.set(0 + lineThickness / 2, 0 + lineThickness / 2, min - lineThickness / 2, min - lineThickness / 2);
    }

    /**
     * Override Method to Draw the actual circular ProgressBar on the view
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawOval(rectF, pBackground);
        float angle = 360 * progress / max;
        canvas.drawArc(rectF, initialAngle, angle, false, pForeground);
    }

    // CTOR
    public CircularProgressBar(Context context, AttributeSet attributes) {
        super(context, attributes);
        init(context, attributes);
    }

    /**
     * Method to set progress animation
     * @param progress state of progress to display
     */
    public void setProgress(float progress) {
        this.progress = progress;
        // Notify the view to redraw itself (the onDraw method is called)
        invalidate();
    }

    /**
     * Method to set circle progress color with Color object
     * @param color foreground color of the circle
     */
    public void setColor(Color color) {
        this.fgColor = color.hashCode();
        pForeground.setColor(color.hashCode());
        // Re-draw the bar
        invalidate();
        // Re-Calculate bounds
        requestLayout();
    }

    /**
     * Method to set circle progress color with integer color value
     * @param color foreground color of the circle
     */
    public void setColor(int color) {
        this.fgColor = color;
        pForeground.setColor(color);
        // Re-draw the bar
        invalidate();
        // Re-Calculate bounds
        requestLayout();
    }

    /**
     * Set the thickness of the circle
     * @param thickness thickness of the circle outline
     */
    public void setLineThickness(float thickness){
        this.lineThickness = thickness;
        this.pBackground.setStrokeWidth(thickness);
        this.pForeground.setStrokeWidth(thickness);
        // Re-draw the bar
        invalidate();
        // Re-Calculate bounds
        requestLayout();
    }

    /**
     * Method to set transparency
     * @param transparencyAmount amount of transparency higher -> more transparent
     */
    public void setBgTransparency(float transparencyAmount) {
        this.bgTransparency = transparencyAmount;
        pBackground.setColor(setTransparent(bgColor));
        pBackground.setStrokeWidth(lineThickness);
        invalidate();
    }

    /**
     * Sets given progress with animation
     * No need to call internal {@link CircularProgressBar#setProgress(float)}} because
     * {@link android.animation.ObjectAnimator} automatically calls the method
     * @param progress State of progress that should be animated to
     * @param duration Time from start to finish of the animation
     */
    public void setAnimatedProgress(float progress, long duration) {

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "progress", progress);
        objectAnimator.setDuration(duration);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.start();
    }

    // Init method
    private void init(Context context, AttributeSet attributes) {
        rectF = new RectF();

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attributes, R.styleable.CircularProgressBar, 0, 0);

        // Get Values from XML (see: values/attributes.xml)
        try{
            lineThickness = typedArray.getDimension(R.styleable.CircularProgressBar_lineThickness, lineThickness);
            progress = typedArray.getFloat(R.styleable.CircularProgressBar_progress, progress);
            min = typedArray.getInt(R.styleable.CircularProgressBar_min, min);
            max = typedArray.getInt(R.styleable.CircularProgressBar_max, max);
        }
        finally{
            typedArray.recycle();
        }

        pBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBackground.setColor(setTransparent(bgColor));
        pBackground.setStyle(Paint.Style.STROKE);
        pBackground.setStrokeWidth(lineThickness);

        pForeground = new Paint(Paint.ANTI_ALIAS_FLAG);
        pForeground.setColor(fgColor);
        pForeground.setStyle(Paint.Style.STROKE);
        pForeground.setStrokeWidth(lineThickness);
    }

    /**
     * Method to make bg color slightly transparent to lighten it
     * @param color Color which should be lightened
     */
    private int setTransparent(int color) {
        int alpha = Math.round(Color.alpha(color) * bgTransparency);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}