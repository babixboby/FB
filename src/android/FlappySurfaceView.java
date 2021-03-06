package com.game.flappybird.plugin;

import com.game.flappybird.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlappySurfaceView extends SurfaceView implements Runnable, SurfaceHolder.Callback, View.OnTouchListener{

    private static final String TAG = "FlappySurfaceView";
    private static final String HIGH_SCORE_PREFERENCE = "SharedPreferences";
    private static final String KEY_HIGH_SCORE = "key_high_score";

    private static final float mYDownAccelerate = 7;

    private Context mContext;

    private enum GameStatus{
        WAITING,
        RUNNING,
        OVER
    }

    private GameStatus mGameStatus;
	private int[] mScores = {R.mipmap.font_0, R.mipmap.font_1, R.mipmap.font_2, R.mipmap.font_3,
                             R.mipmap.font_4, R.mipmap.font_5, R.mipmap.font_6, R.mipmap.font_7,
                             R.mipmap.font_8, R.mipmap.font_9};

    private int mScore;
    private int mHighScore;

    private float mXSpeed;
    private int mScoreIncrease;

    private SurfaceHolder mSurfaceHolder;
    private Canvas mCanvas;
    private Thread mThread;
    private boolean mIsRunning;

    private boolean mDieOnLand;

    private boolean mDieOutsidePipe;

    private boolean mCurPipeIsOne;

    private boolean mNeedUseDownBitmap;
    private boolean mPlusScoreFlag1 = true;
    private boolean mPlusScoreFlag2 = true;

    private Bitmap mDayBgBitmap;
    private Bitmap mLandBitmap;
    private List<Bitmap> mBirdBitmaps = new ArrayList<>();
    private Bitmap mBirdDownBitmap;
    private Bitmap mPipeDownBitmap;
    private Bitmap mPipeUpBitmap;
    private Bitmap mGetReadyBitmap;
    private Bitmap mTutorialBitmap;
    private Bitmap mTitleBitmap;
    private Bitmap mGameOverBitmap;

    private float mViewWidth;
    private float mViewHeight;
    private float mBirdX;
    private float mBirdY;
    private float mBirdWidth;
    private float mBirdHeight;

    private float mBirdWidth2;
    private float mBirdHeight2;
    private float mLandY;
    private float mLandX = 0;
    private float mMinPipeHeight;
    private float mMaxPipeHeight;
    private float mPipeGap;
    private float mPipeX1;
    private float mPipeX2;
    private float mPipeWidth;
    private float mUpPipeHeight1;
    private float mUpPipeHeight2;

    private float mBirdJump;
    private float mBirdDownDis;
    private int mBirdWingAnim;

    public FlappySurfaceView(Context context) {
        this(context, null);
    }

    public FlappySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        setZOrderOnTop(true);
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);

        mGameStatus = GameStatus.WAITING;

        _initResources();
        setOnTouchListener(this);

        Activity activity = (Activity)mContext;
        boolean isSpeedMode = activity.getIntent().getBooleanExtra(MainActivity.EXTRA_IS_SPEED_MODE, false);
        if (isSpeedMode){
            mXSpeed = 20;
            mScoreIncrease = 2;
        } else {
            mXSpeed = 10;
            mScoreIncrease = 1;
        }
    }

    private void _initResources(){
        mDayBgBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.bg_day);
        mLandBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.land);
        Bitmap bird1 = BitmapFactory.decodeResource(getResources(), R.mipmap.bird0_0);
        Bitmap bird2 = BitmapFactory.decodeResource(getResources(), R.mipmap.bird0_1);
        Bitmap bird3 = BitmapFactory.decodeResource(getResources(), R.mipmap.bird0_2);
        mBirdBitmaps.add(bird1);
        mBirdBitmaps.add(bird2);
        mBirdBitmaps.add(bird3);
        mBirdDownBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.bird0_4);
        mPipeDownBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.pipe_down);
        mPipeUpBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.pipe_up);
        mGetReadyBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.get_ready);
        mTutorialBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.tutorial);
        mTitleBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.title);
        mGameOverBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.game_over);
    }

    public void releaseResources(){
        mDayBgBitmap.recycle();
        mLandBitmap.recycle();
        for (Bitmap bitmap : mBirdBitmaps){
            bitmap.recycle();
        }
        mBirdDownBitmap.recycle();
        mPipeDownBitmap.recycle();
        mPipeUpBitmap.recycle();
        mGetReadyBitmap.recycle();
        mTutorialBitmap.recycle();
        mTitleBitmap.recycle();
        mGameOverBitmap.recycle();
        mIsRunning = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mViewHeight = h;
        mViewWidth = w;

        mLandY = h / 5 * 4;

        mBirdWidth = mViewWidth / 10;
        mBirdHeight = mBirdWidth * 0.7f;
        mBirdWidth2 = mBirdHeight * 1.1f;
        mBirdHeight2 = mBirdWidth;
        mBirdY = mLandY / 2 - mBirdHeight;
        mBirdJump = mBirdHeight * 0.6f;

        mPipeGap = mLandY / 5;
        mMinPipeHeight = mPipeGap * 0.5f;
        mMaxPipeHeight = mPipeGap * 3.5f;
        mPipeWidth = mViewWidth / 7;
        mPipeX1 = mViewWidth;
        mPipeX2 = mPipeX1 + mViewWidth / 2 + mPipeWidth / 2;
        mUpPipeHeight1 = new Random().nextFloat() * (mMaxPipeHeight - mMinPipeHeight) + mMinPipeHeight;
        mUpPipeHeight2 = new Random().nextFloat() * (mMaxPipeHeight - mMinPipeHeight) + mMinPipeHeight;
    }

    private void _reset(){
        mDieOnLand = false;
        mDieOutsidePipe = false;
        mCurPipeIsOne = false;
        mNeedUseDownBitmap = false;
        mBirdDownDis = 0;
        mScore = 0;
        mPlusScoreFlag1 = true;
        mPlusScoreFlag2 = true;
        mBirdY = mLandY / 2 - mBirdHeight;
        mPipeX1 = mViewWidth;
        mPipeX2 = mPipeX1 + mViewWidth / 2 + mPipeWidth / 2;
        mUpPipeHeight1 = new Random().nextFloat() * (mMaxPipeHeight - mMinPipeHeight) + mMinPipeHeight;
        mUpPipeHeight2 = new Random().nextFloat() * (mMaxPipeHeight - mMinPipeHeight) + mMinPipeHeight;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new Thread(this);
        mThread.start();
        mIsRunning = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mIsRunning = false;
    }

    @Override
    public void run() {
        while (mIsRunning){
            //long start = System.currentTimeMillis();
			long start = System.nanoTime() / 1000000;
            _calc();
            _draw();
            //long end = System.currentTimeMillis();
			long end = System.nanoTime() / 1000000;
            if (end - start < 50){
                try {
                    mThread.sleep(50 - (end - start));
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (mGameStatus == GameStatus.WAITING){
                    mGameStatus = GameStatus.RUNNING;
                    break;
                } else if (mGameStatus == GameStatus.RUNNING){
                    mBirdDownDis = -mBirdJump;
                    break;
                } else {
                    _reset();
                    mGameStatus = GameStatus.WAITING;
                }
        }

        return true;
    }

    private boolean _isGameOver(){
        if (mBirdY > mLandY - mBirdHeight){
            mBirdY = mLandY - mBirdHeight;
            mDieOnLand = true;
            return true;
        }
        boolean isCrashPipe = _checkPipeCrash(mPipeX1, mUpPipeHeight1);
        if (isCrashPipe){
            mCurPipeIsOne = true;
            return true;
        }
        isCrashPipe = _checkPipeCrash(mPipeX2, mUpPipeHeight2);
        if (isCrashPipe){
            mCurPipeIsOne = false;
            return true;
        }
        return false;
    }

    private boolean _checkPipeCrash(float pipeX, float upPipeHeight){

        if (pipeX > mBirdX + mBirdWidth){
            return false;
        }

        if (pipeX < mBirdX + mBirdWidth && pipeX - mBirdX - mBirdWidth < -mXSpeed
                && pipeX > mBirdX + mBirdWidth - mPipeWidth - mBirdWidth){
            if (mBirdY > upPipeHeight && mBirdY + mBirdHeight < upPipeHeight + mPipeGap){
                return false;
            } else {
                if (mBirdY <= upPipeHeight){
                    mBirdY = upPipeHeight;
                    mNeedUseDownBitmap = true;
                }
                if (mBirdY >= upPipeHeight + mPipeGap - mBirdHeight){
                    mBirdY = upPipeHeight + mPipeGap - mBirdHeight;
                    mNeedUseDownBitmap = false;
                }
                mDieOutsidePipe = false;
                return true;
            }

        } else if (pipeX < mBirdX + mBirdWidth && pipeX - mBirdX - mBirdWidth >= -mXSpeed){
            if (mBirdY > upPipeHeight && mBirdY + mBirdHeight < upPipeHeight + mPipeGap){
                return false;
            } else {
                mDieOutsidePipe = true;
                mNeedUseDownBitmap = true;
                return true;
            }
        }

        return false;
    }

    private void _calc(){
        if (mGameStatus == GameStatus.RUNNING){
            mBirdDownDis += mYDownAccelerate;
            mBirdY += mBirdDownDis;
            if (mBirdY < -mBirdHeight){
                mBirdY = -mBirdHeight;
            }
            if (_isGameOver()){
                mGameStatus = GameStatus.OVER;
                SharedPreferences preferences = mContext.getSharedPreferences(HIGH_SCORE_PREFERENCE, Context.MODE_PRIVATE);
                mHighScore = preferences.getInt(KEY_HIGH_SCORE, 0);
            }
        } else if (mGameStatus == GameStatus.OVER){
            if (mDieOnLand){
                return;
            }
            if (mDieOutsidePipe){
                if (mBirdY + mBirdHeight2 < mLandY){
                    mBirdDownDis += mYDownAccelerate;
                    mBirdY += mBirdDownDis;
                }
                if (mBirdY + mBirdHeight2 >= mLandY){
                    mBirdY = mLandY - mBirdHeight2;
                }
            } else {
                float mCurUpPipeHeight;
                if (mCurPipeIsOne){
                    mCurUpPipeHeight = mUpPipeHeight1;
                } else {
                    mCurUpPipeHeight = mUpPipeHeight2;
                }
                if (mBirdY + mBirdHeight < mCurUpPipeHeight + mPipeGap){
                    mBirdDownDis += mYDownAccelerate;
                    mBirdY += mBirdDownDis;
                }
                if (mBirdY + mBirdHeight2 > mCurUpPipeHeight + mPipeGap){
                    if (mNeedUseDownBitmap){
                        mBirdY = mCurUpPipeHeight + mPipeGap - mBirdHeight2;
                    }else {
                        mBirdY = mCurUpPipeHeight + mPipeGap - mBirdHeight;
                    }
                }
            }
        }
    }

    private void _draw(){
        try{
            mCanvas = mSurfaceHolder.lockCanvas();
            if (mCanvas != null){
                //draw
                _drawBackground();
                _drawBird();
                if (mGameStatus != GameStatus.WAITING){
                    _drawPipe();
                }
                if (mGameStatus == GameStatus.WAITING){
                    _drawReady();
                }
                if (mGameStatus == GameStatus.OVER){
                    _drawGameOver();
                }
                _drawScore(mScore);
                _drawLand();
            }
        } catch (Exception e){
            Log.e(TAG, "_draw() exception occurred!", e);
        } finally {

            if (mCanvas != null){
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        }
    }

    private void _drawBackground(){
        RectF rectF = new RectF(0, 0, mViewWidth, mViewHeight);
        mCanvas.drawBitmap(mDayBgBitmap, null, rectF, null);
    }

    private void _drawLand(){
        if (mGameStatus != GameStatus.OVER){
            mLandX -= mXSpeed;
        }
        if (Math.abs(mLandX) >= mViewWidth){
            mLandX = 0;
        }
        RectF rectF = new RectF(mLandX, mLandY, mViewWidth + mLandX, mViewHeight);
        mCanvas.drawBitmap(mLandBitmap, null, rectF, null);
        rectF.set(mViewWidth + mLandX, mLandY, mViewWidth + mLandX + mViewWidth, mViewHeight);
        mCanvas.drawBitmap(mLandBitmap, null, rectF, null);
    }

    private void _drawBird(){
        mBirdX = mViewWidth / 2 - mBirdWidth;
        RectF rectF = new RectF(mBirdX, mBirdY, mBirdX + mBirdWidth, mBirdY + mBirdHeight);

        if (mGameStatus != GameStatus.OVER){
            mCanvas.drawBitmap(mBirdBitmaps.get(mBirdWingAnim % 3), null, rectF, null);
        } else {
            if (mNeedUseDownBitmap){
                rectF.set(mBirdX, mBirdY, mBirdX + mBirdWidth2, mBirdY + mBirdHeight2);
                mCanvas.drawBitmap(mBirdDownBitmap, null, rectF, null);
            } else {
                mCanvas.drawBitmap(mBirdBitmaps.get(1), null, rectF, null);
            }
        }

        mBirdWingAnim++;
    }

    private void _drawPipe(){
        if (mPipeX1 > -mPipeWidth){
            _drawOnePipe(mPipeX1, mUpPipeHeight1);
        }
        if (mPipeX2 > -mPipeWidth && mPipeX2 < mViewWidth){
            _drawOnePipe(mPipeX2, mUpPipeHeight2);
        }

        if (mPipeX1 <= -mPipeWidth){
            mPipeX1 = mViewWidth;
            mPlusScoreFlag1 = true;
            mUpPipeHeight1 = new Random().nextFloat() * (mMaxPipeHeight - mMinPipeHeight) + mMinPipeHeight;
        }
        if (mPipeX2 <= -mPipeWidth){
            mPipeX2 = mViewWidth;
            mPlusScoreFlag2 = true;
            mUpPipeHeight2 = new Random().nextFloat() * (mMaxPipeHeight - mMinPipeHeight) + mMinPipeHeight;
        }

        SharedPreferences preferences = mContext.getSharedPreferences(HIGH_SCORE_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (mPipeX1 + mPipeWidth < mBirdX && mPlusScoreFlag1){
            mScore += mScoreIncrease;
            mPlusScoreFlag1 = false;
            if (mScore > mHighScore){
                editor.putInt(KEY_HIGH_SCORE, mScore);
                editor.commit();
            }
        }
        if (mPipeX2 + mPipeWidth < mBirdX && mPlusScoreFlag2){
            mScore += mScoreIncrease;
            mPlusScoreFlag2 = false;
            if (mScore > mHighScore){
                editor.putInt(KEY_HIGH_SCORE, mScore);
                editor.commit();
            }
        }

        if (mGameStatus != GameStatus.OVER){
            mPipeX1 -= mXSpeed;
            mPipeX2 -= mXSpeed;
        }
    }

    private void _drawOnePipe(float x, float height){
        RectF rectF = new RectF(x, 0, x + mPipeWidth, mLandY);
        mCanvas.save();
        mCanvas.translate(0, -(mLandY - height));
        mCanvas.drawBitmap(mPipeUpBitmap, null, rectF, null);
        mCanvas.translate(0, mLandY - height + height + mPipeGap);
        mCanvas.drawBitmap(mPipeDownBitmap, null, rectF, null);
        mCanvas.restore();
    }

    private void _drawScore(int score){
        int hundred;
        int decade;
        int unit;
        hundred = score / 100;
        decade = score % 100 / 10;
        unit = score % 10;
        float numWidth = mBirdWidth / 2;
        float numHeight = numWidth * 2;
        float numGap = numWidth / 20;
        float scoreWidth;
        float top = mPipeGap;
        float bottom = mPipeGap + numHeight;
        float center = mViewWidth / 2;
        if (hundred != 0){
            scoreWidth = 3 * numWidth + 2 * numGap;
            RectF rectF = new RectF(center - scoreWidth / 2, top, center - scoreWidth / 2 + numWidth, bottom);
            Bitmap hundredBitmap = BitmapFactory.decodeResource(getResources(), mScores[hundred]);
            mCanvas.drawBitmap(hundredBitmap, null, rectF, null);
            rectF.set(center - numWidth / 2, top, center + numWidth / 2, bottom);
            Bitmap decadeBitmap = BitmapFactory.decodeResource(getResources(), mScores[decade]);
            mCanvas.drawBitmap(decadeBitmap, null, rectF, null);
            rectF.set(center + numWidth / 2 + numGap, top, center + scoreWidth / 2 + numGap, bottom);
            Bitmap unitBitmap = BitmapFactory.decodeResource(getResources(), mScores[unit]);
            mCanvas.drawBitmap(unitBitmap, null, rectF, null);
            hundredBitmap.recycle();
            decadeBitmap.recycle();
            unitBitmap.recycle();
        }
        if (hundred == 0 && decade != 0){
            scoreWidth = 2 * numWidth + numGap;
            RectF rectF = new RectF(center - scoreWidth / 2, top, center - scoreWidth / 2 + numWidth, bottom);
            Bitmap decadeBitmap = BitmapFactory.decodeResource(getResources(), mScores[decade]);
            mCanvas.drawBitmap(decadeBitmap, null, rectF, null);
            rectF.set(center + numGap, top, center + numWidth + numGap, bottom);
            Bitmap unitBitmap = BitmapFactory.decodeResource(getResources(), mScores[unit]);
            mCanvas.drawBitmap(unitBitmap, null, rectF, null);
            decadeBitmap.recycle();
            unitBitmap.recycle();
        }
        if (hundred == 0 && decade == 0){
            scoreWidth = numWidth;
            RectF rectF = new RectF(center - scoreWidth / 2, top, center + scoreWidth / 2, bottom);
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mScores[unit]);
            mCanvas.drawBitmap(bitmap, null, rectF, null);
            bitmap.recycle();
        }
    }

    private void _drawReady(){
        float center = mViewWidth / 2;

        float getReadyWidth = mPipeWidth * 3;
        float getReadyHeight = mPipeGap * 0.7f;
        float getReadyY = mBirdY + mBirdHeight * 2;
        RectF rectF = new RectF(center - getReadyWidth / 2, getReadyY, center + getReadyWidth / 2, getReadyY + getReadyHeight);
        mCanvas.drawBitmap(mGetReadyBitmap, null, rectF, null);

        float tutorialWidth = getReadyWidth;
        float tutorialHeight = getReadyHeight * 2;
        float tutorialY = getReadyY + getReadyHeight;
        rectF.set(center - tutorialWidth / 2, tutorialY, center + tutorialWidth / 2, tutorialY + tutorialHeight);
        mCanvas.drawBitmap(mTutorialBitmap, null, rectF, null);

        float titleWidth = getReadyWidth * 1.2f;
        float titleHeight = getReadyHeight * 0.6f;
        float titleY = mPipeGap * 0.4f;
        rectF.set(center - titleWidth / 2, titleY, center + titleWidth / 2, titleY + titleHeight);
        mCanvas.drawBitmap(mTitleBitmap, null, rectF, null);
    }

    private void _drawGameOver(){
        float center = mViewWidth / 2;

        float panelWidth = mViewWidth * 0.65f;
        float panelHeight = panelWidth * 0.45f;
        float panelY = mLandY / 2;
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.shape_score_panel);
        drawable.setBounds((int)(center - panelWidth / 2), (int)panelY, (int)(center + panelWidth / 2), (int)(panelY + panelHeight));
        drawable.draw(mCanvas);

        float gameOverWidth = mPipeWidth * 3;
        float gameOverHeight = mPipeGap * 0.6f;
        float gameOverY = mLandY / 2;

        RectF rectF = new RectF(center - gameOverWidth / 2, gameOverY + mBirdHeight* 0.3f, center + gameOverWidth / 2, gameOverY + mBirdHeight* 0.3f + gameOverHeight);
        mCanvas.drawBitmap(mGameOverBitmap, null, rectF, null);

        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(mBirdHeight);
        Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        paint.setTypeface(font);
        paint.setColor(mContext.getResources().getColor(R.color.score_color));
        mCanvas.drawText("score:" + mScore + "  best:" + mHighScore, center, gameOverY + gameOverHeight + mBirdHeight * 1.2f, paint);
    }

}
