/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mych.cloudgameclient.decoder;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.mych.cloudgameclient.android.R;
import com.mych.cloudgameclient.media.IMediaExtractor;
import com.mych.cloudgameclient.media.MediaCodecWrapper;
import com.mych.cloudgameclient.media.MyH264Extractor;

/**
 * This activity uses a {@link android.view.TextureView} to render the frames of a video decoded using
 * {@link android.media.MediaCodec} API.
 */
public class PlayerActivity extends Activity {

	private TextureView mPlaybackView;
    private TimeAnimator mTimeAnimator = new TimeAnimator();

    // A utility that wraps up the underlying input and output buffer processing operations
    // into an east to use API.
    private MediaCodecWrapper mCodecWrapper;
    private MediaExtractor mExtractor2 = new MediaExtractor();
    private IMediaExtractor mExtractor = new MyH264Extractor();
    TextView mAttribView = null;

    // video output dimension
    static final int OUTPUT_WIDTH = 1280;
    static final int OUTPUT_HEIGHT = 720;

    VideoDecoder mDecoder;
    AudioPlayer mAudioPlayer;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(outputStream == null){
            return super.dispatchKeyEvent(event);
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(5);
        
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            buffer.put((byte)0x08);
        	///input[4] = 3;
        }else if(event.getAction() == KeyEvent.ACTION_UP){
        	buffer.put((byte)0x09);
        	///input[4] = 4;
        }

        ///ByteBuffer keyCode = ByteBuffer.wrap(input, 5, 4);
        ///keyCode.order(ByteOrder.LITTLE_ENDIAN).putInt(event.getKeyCode());

        buffer.putInt(event.getKeyCode());
        
        System.out.print("send input:" + event.getKeyCode());

        try {
            outputStream.write(buffer.array());
        } catch (IOException e) {
            System.out.print(" fail\n");
        }

        return true;
    }
    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player);
        mPlaybackView = (TextureView) findViewById(R.id.PlaybackView);
        mAttribView =  (TextView)findViewById(R.id.AttribView);
        
        mDecoder = new VideoDecoder();
        mAudioPlayer = new AudioPlayer("");
        
        mPlaybackView.setSurfaceTextureListener(new SurfaceTextureListener(){

			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				mAttribView.setVisibility(View.VISIBLE);
		        startPlayback();
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				return false;
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			}});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mTimeAnimator != null && mTimeAnimator.isRunning()) {
            mTimeAnimator.end();
        }

        if (mCodecWrapper != null ) {
            mCodecWrapper.stopAndRelease();
            mExtractor.release();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_play) {
            mAttribView.setVisibility(View.VISIBLE);
            startPlayback();
            item.setEnabled(false);
        }
        return true;
    }

    private OutputStream outputStream = null;
    
    public void startPlayback1(){
    	//mAudioPlayer.start(this); 
    	//mAudioPlayer.run();
    }
    
    public void startPlayback(){

    	mDecoder.start();

        mDecoder.configure(new Surface(mPlaybackView.getSurfaceTexture()), 1280, 720, new byte[]{}, 0, 0);

        final Context ctx = this;

        Thread mFeedWorker = new Thread(new Runnable(){
            @Override
            public void run() {

                /* 服务器地址 */
                final String SERVER_HOST_IP = "192.168.1.13";
                /* 服务器端口 */
                final int SERVER_HOST_PORT = 8888;

                Socket socket;
                InputStream stream = null;

                /* 连接服务器 */
                try {
                    socket = new Socket(SERVER_HOST_IP, SERVER_HOST_PORT);
                    stream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                }catch (IOException e){
                    ///Toast.makeText(getApplicationContext(), "连接源失�?", Toast.LENGTH_SHORT).show();
                    return;
                }

                /*try {
                    stream = ctx.getContentResolver().openInputStream(Uri.parse("android.resource://"
                            + getPackageName() + "/"
                            //+ R.raw.vid_bigbuckbunny);
                            + R.raw.test));
                }catch (FileNotFoundException e){

                }*/

                byte[] frameSample = new byte[1280 * 720];
                while(true){
                	
                    try {
                    	if(stream == null){
                    		Thread.sleep(10000);
                    		continue;
                    	}
                    	
                    	
                        byte[] head = new byte[]{0, 0, 0, 0};
                        if(4 != stream.read(head)){
                            break;
                        }
                        int length = (0xFF & head[3]) << 24 | (0xFF & head[2]) << 16 | (0xFF & head[1]) << 8 | (0xFF & head[0]);
                        System.out.println("length: " + length);
                        //TODO:根据header判断video还是audio并调用相关解码器
                        int more = length;
                        //byte[] type = new byte[]{0};
                        while(more > 0) {
                        	//stream.read(type);
                            int ret = stream.read(frameSample, length - more, more);
                            if(ret > 0){
                                more -= ret;
                            }else if(ret < 0){
                                break;
                            }
                        }

                        if(more != 0){
                            System.out.println("receive length: " + (length - more));
                            return;
                        }

                        mDecoder.decodeSample(frameSample, 1, length - 1, System.currentTimeMillis(), 0);

                        ///Thread.sleep(20);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mFeedWorker.start();

        // Construct a URI that points to the video resource that we want to play
        /*Uri videoUri = Uri.parse("android.resource://"
                + getPackageName() + "/"
                //+ R.raw.vid_bigbuckbunny);
                + R.raw.test);

        try {

            // BEGIN_INCLUDE(initialize_extractor)
            mExtractor.setDataSource(this, videoUri, null);
            ///int nTracks = mExtractor.getTrackCount();

            // Begin by unselecting all of the tracks in the extractor, so we won't see
            // any tracks that we haven't explicitly selected.

            MediaFormat format = MediaFormat.createVideoFormat("video/AVC", 1280, 720);

            // Find the first video track in the stream. In a real-world application
            // it's possible that the stream would contain multiple tracks, but this
            // sample assumes that we just want to play the first one.
            //for (int i = 0; i < nTracks; ++i) {
            // Try to create a video codec for this track. This call will return null if the
            // track is not a video track, or not a recognized video format. Once it returns
            // a valid MediaCodecWrapper, we can break out of the loop.
            mCodecWrapper = MediaCodecWrapper.fromVideoFormat(format, //mExtractor.getTrackFormat(i),
                    new Surface(mPlaybackView.getSurfaceTexture()));
            if (mCodecWrapper != null) {
                //mExtractor.selectTrack(i);
                //break;
            } else {
                return;
            }
            //}
            // END_INCLUDE(initialize_extractor)


            // By using a {@link TimeAnimator}, we can sync our media rendering commands with
            // the system display frame rendering. The animator ticks as the {@link Choreographer}
            // recieves VSYNC events.
            mTimeAnimator.setTimeListener(new TimeAnimator.TimeListener() {
                @Override
                public void onTimeUpdate(final TimeAnimator animation,
                                         final long totalTime,
                                         final long deltaTime) {

                    // Try to submit the sample to the codec and if successful advance the
                    // extractor to the next available sample to read.

                    boolean result = mCodecWrapper.writeSample(mExtractor, false,
                            System.currentTimeMillis(), 0);

                    if (result) {
                        System.out.println("PTS: " + System.currentTimeMillis());
                        // Advancing the extractor is a blocking operation and it MUST be
                        // executed outside the main thread in real applications.
                        mExtractor.advance();
                    } else {
                        ///System.out.println("result " + result);
                    }
                    //}
                    // END_INCLUDE(write_sample)

                    // Examine the sample at the head of the queue to see if its ready to be
                    // rendered and is not zero sized End-of-Stream record.
                    MediaCodec.BufferInfo out_bufferInfo = new MediaCodec.BufferInfo();
                    mCodecWrapper.peekSample(out_bufferInfo);

                    // BEGIN_INCLUDE(render_sample)
                    if (out_bufferInfo.size <= 0 && false) {
                        mTimeAnimator.end();
                        mCodecWrapper.stopAndRelease();
                        mExtractor.release();
                    } else if (out_bufferInfo.presentationTimeUs / 1000 < totalTime) {
                        // Pop the sample off the queue and send it to {@link Surface}
                        mCodecWrapper.popSample(true);


                    }
                    // END_INCLUDE(render_sample)

                }
            });

            // We're all set. Kick off the animator to process buffers and render video frames as
            // they become available
            mTimeAnimator.start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }
}
