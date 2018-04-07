package com.prohua.tvblive;

import android.content.pm.ActivityInfo;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.prohua.universal.UniversalAdapter;
import com.prohua.universal.UniversalViewHolder;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements ITXLivePlayListener {

    private final static String RTMP_URL = "http://acm.gg/jade.m3u8";

    // 播放器
    private TXLivePlayer mLivePlayer = null;
    // 视图
    private TXCloudVideoView mPlayerView;
    // 播放器配置
    private TXLivePlayConfig mPlayConfig;
    // 硬解
    private boolean mHWDecode = false;
    // 显示模式
    private int mCurrentRenderMode;
    // 显示方向
    private int mCurrentRenderRotation;

    // RTMP直播流
    private int mPlayType = TXLivePlayer.PLAY_TYPE_VOD_HLS;

    private RecyclerView recyclerView;

    private UniversalAdapter universalAdapter;

    private List<String> stringList;

    PowerManager powerManager = null;
    PowerManager.WakeLock wakeLock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        powerManager = (PowerManager) this.getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");

        initView();
    }

    private void initView() {
        // 比例放大
        mCurrentRenderMode = TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION;
        // 不旋转
        mCurrentRenderRotation = TXLiveConstants.RENDER_ROTATION_PORTRAIT;

        // 初始化配置
        mPlayConfig = new TXLivePlayConfig();

        // 初始化直播播放器
        if (mLivePlayer == null) {
            mLivePlayer = new TXLivePlayer(getBaseContext());
        }

        mPlayerView = (TXCloudVideoView) findViewById(R.id.video_view);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        stringList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        universalAdapter = new UniversalAdapter(getBaseContext(), stringList, R.layout.main_list_item_layout, 0, 0);

        universalAdapter.setOnBindItemView(new UniversalAdapter.OnBindItemView() {
            @Override
            public void onBindItemViewHolder(UniversalViewHolder universalViewHolder, int i) {
                universalViewHolder.setText(R.id.textContent, stringList.get(i));
            }
        });

        recyclerView.setAdapter(universalAdapter);

        startPlay();
    }

    private boolean startPlay() {

        mLivePlayer.setPlayerView(mPlayerView);

        mLivePlayer.setPlayListener(this);
        // 硬件加速在1080p解码场景下效果显著，但细节之处并不如想象的那么美好：
        // (1) 只有 4.3 以上android系统才支持
        // (2) 兼容性我们目前还仅过了小米华为等常见机型，故这里的返回值您先不要太当真
        mLivePlayer.enableHardwareDecode(mHWDecode);
        mLivePlayer.setRenderRotation(mCurrentRenderRotation);
        mLivePlayer.setRenderMode(mCurrentRenderMode);
        //设置播放器缓存策略
        //这里将播放器的策略设置为自动调整，调整的范围设定为1到4s，您也可以通过setCacheTime将播放器策略设置为采用
        //固定缓存时间。如果您什么都不调用，播放器将采用默认的策略（默认策略为自动调整，调整范围为1到4s）
        //mLivePlayer.setCacheTime(5);

        mLivePlayer.setConfig(mPlayConfig);

        int result = mLivePlayer.startPlay(RTMP_URL, mPlayType);
        // result返回值：0 success;  -1 empty url; -2 invalid url; -3 invalid playType;
        if (result != 0) {
            return false;
        }

        //stopAnim();

        Log.w("video render", "timetrack start play");

        return true;
    }

    @Override
    public void onPlayEvent(int i, Bundle bundle) {
        Log.i("事件", "" + i);

        stringList.add("事件: " + i);
        if (i == TXLiveConstants.PLAY_EVT_CONNECT_SUCC) {
            Log.i("事件", "已经连接服务器");
            stringList.add("事件: " + i + " 已经连接服务器");
        } else if (i == TXLiveConstants.PLAY_EVT_RTMP_STREAM_BEGIN) {
            Log.i("事件", "已经连接服务器，开始拉流");
            stringList.add("事件: " + i + " 已经连接服务器，开始拉流");
        } else if (i == TXLiveConstants.PLAY_EVT_RCV_FIRST_I_FRAME) {
            Log.i("事件", "网络接收到首个可渲染的视频数据包");
            stringList.add("事件: " + i + " 网络接收到首个可渲染的视频数据包");
        } else if (i == TXLiveConstants.PLAY_EVT_PLAY_BEGIN) {
            Log.i("事件", "视频播放开始");
            stringList.add("事件: " + i + " 视频播放开始");
            recyclerView.setVisibility(View.GONE);
        } else if (i == TXLiveConstants.PLAY_EVT_PLAY_END
                || i == TXLiveConstants.PLAY_ERR_NET_DISCONNECT) {
            Log.i("事件", "视频播放结束");
            stringList.add("事件: " + i + " 视频播放结束");
        }
        universalAdapter.notifyDataSetChanged();
    }

    @Override
    public void onNetStatus(Bundle bundle) {

    }

    @Override
    public void onResume() {

        super.onResume();

        if(mLivePlayer != null) {
            mLivePlayer.resume();
        }
        if(wakeLock != null) {
            wakeLock.acquire();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mLivePlayer != null) {
            mLivePlayer.pause();
        }
        if(wakeLock != null) {
            wakeLock.release();
        }
    }
}
