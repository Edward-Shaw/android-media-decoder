package com.mych.cloudgameclient.media;

import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by Gang on 2016/4/16.
 */
public class MyH264Extractor implements IMediaExtractor {
    private InputStream stream = null;
    @Override
    public void release() {

    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) {
        try {
            stream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
        }
    }

    @Override
    public boolean advance() {
        return true;
    }

    @Override
    public int readSampleData(ByteBuffer buffer, int argv1) {
        try {
            byte[] head = new byte[]{0, 0, 0, 0};
            if(4 != stream.read(head)){
                return 0;
            }
            int length = (0xFF & head[3]) << 24 | (0xFF & head[2]) << 16 | (0xFF & head[1]) << 8 | (0xFF & head[0]);
            byte[] temp = new byte[length];

            int ret = stream.read(temp, 0, length);
            if(ret > 0){
                buffer.put(temp);
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
