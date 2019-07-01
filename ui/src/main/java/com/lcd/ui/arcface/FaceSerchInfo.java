package com.lcd.ui.arcface;

import com.lcd.ui.arcface.faceserver.CompareResult;

public class FaceSerchInfo {
    private byte[] mFeatureData;
    private CompareResult mCompareResult;

    public FaceSerchInfo(byte[] bytes, CompareResult compareresult){
        mFeatureData = bytes;
        mCompareResult = compareresult;
    }

    public void setFeatureData(byte[] bytes){
        mFeatureData = bytes;
    }

    public byte[] getFeatureData(){
        return mFeatureData;
    }

    public void setCompareResult(CompareResult compareresult){
        mCompareResult = compareresult;
    }

    public CompareResult getCompareResult(){
        return mCompareResult;
    }
}
