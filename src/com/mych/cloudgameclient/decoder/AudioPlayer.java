package com.mych.cloudgameclient.decoder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.mych.cloudgameclient.android.R;

public class AudioPlayer implements Runnable {

    private MediaCodec mCodec;
    ///private MediaExtractor mExtractor;
    ///private String mPath;
    private AudioTrack mAudioTrack = null;
    private int mBufferSize = 0;

    private float mRelativePlaybackSpeed = 1.f;
    private int mSrcRate = 44100;
    private boolean isPlaying = false;
    private boolean doStop = false;
    
    private final static long TIMEOUT_US = 1000;

    public AudioPlayer(String path) {
        ///mPath = path;
    }

    private InputStream mStream;
    
    public void start(final Context ctx) {
    	
    	System.out.println("start \n");
    	
    	MediaFormat format = MediaFormat.createAudioFormat("audio/mpeg", 44100, 2);
        ///String mime = format.getString(MediaFormat.MIMETYPE_AUDIO_MPEG);
        
        ///System.out.println("mime: " + mime);
        
        //if (mime.startsWith("audio")) {
            try {
				mCodec = MediaCodec.createDecoderByType("audio/mpeg");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            mCodec.configure(format,
                    null, // We don't have a surface in audio decoding
                    null, // No crypto
                    0); // 0 for decoding

            mCodec.start(); // Fire up the codec
            // Create an AudioTrack. Don't make the buffer size too small:
            mBufferSize = 8 * AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    44100,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    mBufferSize,
                    AudioTrack.MODE_STREAM);
            // Don't forget to start playing
            mAudioTrack.play();
        ///}
        
    	
    	try {
			mStream = ctx.getContentResolver().openInputStream(Uri.parse("android.resource://"
			        + ctx.getPackageName() + "/"
			        //+ R.raw.vid_bigbuckbunny);
			        + R.raw.chuanqi));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
        isPlaying = true;
        doStop = false;
        
        new Thread(this).start();
    }
    
    public void stop() {
        doStop = true;
    }

    public void run() {
    	      	
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        ByteBuffer[] outBuffers = mCodec.getOutputBuffers();
        ByteBuffer activeOutBuffer = null; // The active output buffer
        int activeIndex = 0; // Index of the active buffer

        int availableOutBytes = 0;
        int writeableBytes = 0;
        // writeBuffer stores the samples until they can be written out to the AudioTrack
        final byte[] writeBuffer = new byte[mBufferSize];
        int writeOffset = 0;

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        boolean EOS = false;
        
        while (!Thread.interrupted() && !doStop) {
            // Get PCM data from the stream
            if (!EOS) {
                // Dequeue an input buffer
                int inIndex = mCodec.dequeueInputBuffer(TIMEOUT_US);
                if (inIndex >= 0) {
                    ByteBuffer buffer = inputBuffers[inIndex];
                    buffer.flip();
                    // Fill the buffer with stream data
                    int sampleSize = 0;
					try {
						sampleSize = mStream.read(buffer.array());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    // Pass the stream data to the codec for decoding: queueInputBuffer 
                    if (sampleSize < 0) {
                        // We have reached the end of the stream
                        mCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        EOS = true;
                    } else {
                        mCodec.queueInputBuffer(inIndex, 0, sampleSize, System.currentTimeMillis(), 0);
                    }
                }
            }

            if (availableOutBytes == 0) {
                // we don't have any samples available: Dequeue a new output buffer.
                activeIndex = mCodec.dequeueOutputBuffer(info, TIMEOUT_US);

                // outIndex might carry some information for us.
                switch (activeIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    outBuffers = mCodec.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    // Update the playback rate
                    MediaFormat outFormat = mCodec.getOutputFormat();
                    mSrcRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    mAudioTrack.setPlaybackRate((int) (mSrcRate * mRelativePlaybackSpeed));
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    // Nothing to do
                    break;
                default:
                    // set the activeOutBuffer
                    activeOutBuffer = outBuffers[activeIndex];
                    availableOutBytes = info.size;
                    assert info.offset == 0;
                }
            }

            if (activeOutBuffer != null && availableOutBytes > 0) {
                writeableBytes = Math.min(availableOutBytes, mBufferSize - writeOffset);
                // Copy as many samples to writeBuffer as possible
                activeOutBuffer.get(writeBuffer, writeOffset, writeableBytes);
                availableOutBytes -= writeableBytes;
                writeOffset += writeableBytes;
            }

            if (writeOffset == mBufferSize) {
                // The buffer is full. Submit it to the AudioTrack
                mAudioTrack.write(writeBuffer, 0, mBufferSize);
                writeOffset = 0;
            }

            if (activeOutBuffer != null && availableOutBytes == 0) {
                // IMPORTANT: Clear the active buffer!
                activeOutBuffer.clear();
                if (activeIndex >= 0) {
                    // Give the buffer back to the codec
                    mCodec.releaseOutputBuffer(activeIndex, false);
                }
            }

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                // Get out of here
                break;
            }
        }

        //Clean up
        isPlaying = false;
        doStop = false;
        mCodec.stop();
        mCodec.release();
    }

    public void setRelativePlaybackSpeed(float speed) {
        mRelativePlaybackSpeed = speed;
        if (mAudioTrack != null) {
            mAudioTrack.setPlaybackRate((int) (mSrcRate * mRelativePlaybackSpeed));
        }
    }

    public void setVolume(float vol) {
        if (mAudioTrack != null) {
            mAudioTrack.setStereoVolume(vol, vol);
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}

