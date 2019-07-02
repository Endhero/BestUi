package com.lcd.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.lcd.ui.arcface.ConfigUtil;
import com.lcd.ui.arcface.DrawHelper;
import com.lcd.ui.arcface.DrawInfo;
import com.lcd.ui.arcface.FacePreviewInfo;
import com.lcd.ui.arcface.FaceSerchInfo;
import com.lcd.ui.arcface.camera.CameraHelper;
import com.lcd.ui.arcface.camera.CameraListener;
import com.lcd.ui.arcface.face.FaceHelper;
import com.lcd.ui.arcface.face.FaceListener;
import com.lcd.ui.arcface.face.RequestFeatureStatus;
import com.lcd.ui.arcface.faceserver.CompareResult;
import com.lcd.ui.arcface.faceserver.FaceServer;
import com.lcd.ui.arcface.view.FaceRectView;
import com.lcd.ui.common.Const;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class FaceDetectView extends FrameLayout implements ViewTreeObserver.OnGlobalLayoutListener{
    public static final int ASF_OP_0_HIGHER_EXT = FaceEngine.ASF_OP_0_HIGHER_EXT;
    public static final int ASF_OP_0_ONLY = FaceEngine.ASF_OP_0_ONLY;
    public static final int ASF_OP_90_ONLY = FaceEngine.ASF_OP_90_ONLY;
    public static final int ASF_OP_180_ONLY = FaceEngine.ASF_OP_180_ONLY;
    public static final int ASF_OP_270_ONLY = FaceEngine.ASF_OP_270_ONLY;

    private static final String TAG = "RegisterAndRecognize";
    private static final int MAX_DETECT_NUM = 10;
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 50;
    private static final float SIMILAR_THRESHOLD = 0.8F;

    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;

    /**
     * 优先打开的摄像头
     */
    private Integer cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private FaceEngine faceEngine;
    private FaceHelper faceHelper;
    private FaceDetectListener mFaceDetectListener;

    /**
     * 活体检测的开关
     */
    private boolean mLiveNessDetect = true;
    private int registerStatus = REGISTER_STATUS_DONE;
    private int afCode = -1;
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();

    private FaceRectView mFaceRectView;
    private TextureView mTextureView;
    private Context mContext;
    private boolean mStartDetect;
    private byte[] mData;
    private int mSearchStatus = -1;
    private long mStartDetectTime;

    public FaceDetectView(Context context) {
        super(context);

        initView(context);
    }

    public FaceDetectView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initView(context);
    }

    public FaceDetectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initView(context);
    }

    private void initView(Context context){
        mContext = context;
        mFaceRectView = new FaceRectView(mContext);
        mTextureView = new TextureView(mContext);

        LayoutParams layoutparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(mTextureView, layoutparams);
        addView(mFaceRectView,layoutparams);

        mTextureView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    public void init(){
        FaceServer.getInstance().init(mContext);
        initEngine();
        initCamera();
    }

    private void initEngine() {
        faceEngine = new FaceEngine();
        afCode = faceEngine.init(mContext, FaceEngine.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(mContext),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_LIVENESS);
        VersionInfo versionInfo = new VersionInfo();
        faceEngine.getVersion(versionInfo);
        Log.i(TAG, "initEngine:  init: " + afCode + "  version:" + versionInfo);

        if (afCode != ErrorInfo.MOK) {
            Log.e(TAG, "引擎初始化失败，错误码" + afCode);
        }
    }

    public void setFaceDetectListener(FaceDetectListener facedetectlistener){
        mFaceDetectListener = facedetectlistener;
    }

    public void setLiveNessDetect(boolean livenessdetect){
        mLiveNessDetect = livenessdetect;
    }

    private void initCamera() {
        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                Log.e(TAG, "onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId) {
                //FR成功
                if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);

                    //不做活体检测的情况，直接搜索
                    if (!mLiveNessDetect) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测通过，搜索特征
                    else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测未出结果，延迟100ms再执行该函数
                    else if (livenessMap.get(requestId) != null && livenessMap.get(requestId) == LivenessInfo.UNKNOWN) {
                        getFeatureDelayedDisposables.add(Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                .subscribe(new Consumer<Long>() {
                                    @Override
                                    public void accept(Long aLong) {
                                        onFaceFeatureInfoGet(faceFeature, requestId);
                                    }
                                }));
                    }
                    //活体检测失败
                    else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.NOT_ALIVE);

                        if (mFaceDetectListener != null){
                            mFaceDetectListener.onFaceFeatureInfoGetFail();
                        }
                    }
                }
                //FR 失败
                else {
                    requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                }
            }
        };

        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, mTextureView.getWidth(), mTextureView.getHeight(), displayOrientation
                        , cameraId, isMirror);

                faceHelper = new FaceHelper.Builder()
                        .faceEngine(faceEngine)
                        .frThreadNum(1)
                        .previewSize(previewSize)
                        .faceListener(faceListener)
                        .currentTrackId(ConfigUtil.getTrackId(mContext.getApplicationContext()))
                        .build();
            }

            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (mFaceDetectListener != null){
                    mFaceDetectListener.onPreview(FaceDetectView.this, mTextureView, mFaceRectView);
                }

                if (mFaceRectView != null) {
                    mFaceRectView.clearFaceInfo();
                }

                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && mFaceRectView != null && drawHelper != null) {
                    List<DrawInfo> drawInfoList = new ArrayList<>();
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        drawInfoList.add(new DrawInfo(facePreviewInfoList.get(i).getFaceInfo().getRect(), GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, LivenessInfo.UNKNOWN,
                                ""));
                    }

                    drawHelper.draw(mFaceRectView, drawInfoList);
                }

                if (mSearchStatus != RequestFeatureStatus.SEARCHING){
                    mData = nv21;
                }

                requestFaceFeature(nv21, facePreviewInfoList);

                clearLeftFace(facePreviewInfoList);
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(mTextureView.getMeasuredWidth(),mTextureView.getMeasuredHeight()))
                .rotation(((Activity)mContext).getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(cameraID != null ? cameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(mTextureView)
                .cameraListener(cameraListener)
                .build();

        cameraHelper.init();

        if (mFaceDetectListener != null){
            mFaceDetectListener.onPrepareCamera(this, mTextureView, mFaceRectView);
        }
    }
    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        Set<Integer> keySet = requestFeatureStatusMap.keySet();

        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            return;
        }

        for (Integer integer : keySet) {
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == integer) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(integer);
                livenessMap.remove(integer);
            }
        }
    }

    public void registerFace(final byte[] nv21){
        if (registerStatus == REGISTER_STATUS_READY && mStartDetect) {
            registerStatus = REGISTER_STATUS_PROCESSING;

            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) {
                    boolean success = FaceServer.getInstance().register(mContext, nv21.clone(), previewSize.width, previewSize.height, "registered " + faceHelper.getCurrentTrackId());

                    emitter.onNext(success);
                    emitter.onComplete();
                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            if (mFaceDetectListener != null) {
                                if (success){
                                    mFaceDetectListener.onFaceRegisterSuccess("registered " + faceHelper.getCurrentTrackId());
                                    registerStatus = REGISTER_STATUS_DONE;
                                }else {
                                    mFaceDetectListener.onFaceRegisterFail();
                                    registerStatus = REGISTER_STATUS_READY;
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            registerStatus = REGISTER_STATUS_READY;

                            if (mFaceDetectListener != null){
                                mFaceDetectListener.onFaceRegisterFail();
                            }
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }
    private void requestFaceFeature(byte[] nv21,List<FacePreviewInfo> facePreviewInfoList){
        mSearchStatus = RequestFeatureStatus.SEARCHING;

        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            if (mLiveNessDetect) {
                livenessMap.put(facePreviewInfoList.get(i).getTrackId(), facePreviewInfoList.get(i).getLivenessInfo().getLiveness());
            }

            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
        }
    }

    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        if (mStartDetect){
            if (mStartDetectTime == 0){
                mStartDetectTime = System.currentTimeMillis();
            }

            Observable
                    .create(new ObservableOnSubscribe<CompareResult>() {
                        @Override
                        public void subscribe(ObservableEmitter<CompareResult> emitter) {
                            CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);

                            if (compareResult == null) {
                                emitter.onError(null);
                            } else {
                                emitter.onNext(compareResult);
                                emitter.onComplete();
                            }
                        }
                    })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<CompareResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(CompareResult compareResult) {
                            if (compareResult.getUserName() == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                faceHelper.addName(requestId, "VISITOR " + requestId);

                                if (mFaceDetectListener != null){
                                    FaceSerchInfo faceserchinfo = new FaceSerchInfo(mData, compareResult);
                                    mFaceDetectListener.onFaceSearchFail(faceserchinfo);
                                }

                                mSearchStatus = RequestFeatureStatus.FAILED;

                                return;
                            }

//                        Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                            if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
                                compareResult.setTrackId(requestId);
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                                faceHelper.addName(requestId, compareResult.getUserName());

                                if (mFaceDetectListener != null){
                                    FaceSerchInfo faceserchinfo = new FaceSerchInfo(mData, compareResult);
                                    mFaceDetectListener.onFaceSearchSuccess(faceserchinfo);
                                }

                                mSearchStatus = RequestFeatureStatus.SUCCEED;
                            } else {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                faceHelper.addName(requestId,"VISITOR " + requestId);

                                if (mFaceDetectListener != null){
                                    FaceSerchInfo faceserchinfo = new FaceSerchInfo(mData, compareResult);
                                    mFaceDetectListener.onFaceSearchFail(faceserchinfo);
                                }

                                mSearchStatus = RequestFeatureStatus.FAILED;
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);

                            if (mFaceDetectListener != null){
                                FaceSerchInfo faceserchinfo = new FaceSerchInfo(mData, null);
                                mFaceDetectListener.onFaceSearchError(e, faceserchinfo);
                            }

                            mSearchStatus = RequestFeatureStatus.FAILED;
                        }

                        @Override
                        public void onComplete() {
                        }
                    });
        }
    }

    public static void activeEngine(final Context context){
        if (!ConfigUtil.getActive(context)){
            Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                    FaceEngine faceEngine = new FaceEngine();
                    int activeCode = faceEngine.active(context, Const.ARCFACE_APP_ID, Const.ARCFACE_SDK_KEY);
                    emitter.onNext(activeCode);
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        } @Override
                        public void onNext(Integer activeCode) {
                            if (activeCode == ErrorInfo.MOK) {
                                ConfigUtil.setActive(context, true);
//                                view.showToast(this.getString(R.string.active_success));
                            } else if (activeCode == ErrorInfo.MERR_ASF_ACTIVATION_FAIL) {
//                                view.showToast(this.getString(R.string.active_failed));
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
//                            view.showToast(e.getMessage());
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    /**
     * 销毁引擎
     */
    private void unInitEngine() {

        if (afCode == ErrorInfo.MOK) {
            afCode = faceEngine.unInit();
            Log.i(TAG, "unInitEngine: " + afCode);
        }
    }

    /**
     *      * 将准备注册的状态置为
     *      *
     */
    public void register() {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY;
        }
    }

    public void setFtOrient(int nFtOrient){
        ConfigUtil.setFtOrient(getContext(),nFtOrient);
    }

    public void startDetect(boolean bStartDetect){
        mStartDetect = bStartDetect;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }

        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            synchronized (faceHelper) {
                unInitEngine();
            }
            ConfigUtil.setTrackId(mContext, faceHelper.getCurrentTrackId());
            faceHelper.release();
        } else {
            unInitEngine();
        }
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.dispose();
            getFeatureDelayedDisposables.clear();
        }

        FaceServer.getInstance().unInit();
    }

    @Override
    public void onGlobalLayout() {
        mTextureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

        init();
    }

    public long getStartDetectTime() {
        return mStartDetectTime;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setRadius(float fRadius){
        setOutlineProvider(new FaceDetectViewOutlineProvider(fRadius));
    }

    public interface FaceDetectListener{
        void onFaceRegisterSuccess(String strName);

        void onFaceRegisterFail();

        void onFaceSearchSuccess(FaceSerchInfo faceserchinfo);

        void onFaceSearchFail(FaceSerchInfo faceserchinfo);

        void onFaceSearchError(Throwable throwable, FaceSerchInfo faceserchinfo);

        void onPreview(FaceDetectView facedetectview, TextureView textureview, FaceRectView facerectview);

        void onPrepareCamera(FaceDetectView facedetectview, TextureView textureview, FaceRectView facerectview);

        void onFaceFeatureInfoGetFail();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class FaceDetectViewOutlineProvider extends ViewOutlineProvider {
        private float mRadius;

        public FaceDetectViewOutlineProvider(float radius) {
            this.mRadius = radius;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            Rect rect = new Rect();
            view.getGlobalVisibleRect(rect);
            int leftMargin = 0;
            int topMargin = 0;
            Rect selfRect = new Rect(leftMargin, topMargin,
                    rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);
            outline.setRoundRect(selfRect, mRadius);
        }
    }
}
