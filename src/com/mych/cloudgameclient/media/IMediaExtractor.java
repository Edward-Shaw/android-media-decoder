package com.mych.cloudgameclient.media;

import android.content.Context;
import android.net.Uri;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by Gang on 2016/4/16.
 */
public interface IMediaExtractor {
    void release();

    void setDataSource(Context context, Uri uri, Map<String, String> headers);

    boolean advance();

    int readSampleData(ByteBuffer buffer, int argv1);
}
