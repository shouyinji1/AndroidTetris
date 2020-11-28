package com.uestc.androidtetris;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.util.Timer;
import java.util.TimerTask;

import static com.uestc.androidtetris.R.id.maxScore;


public class MainActivity extends AppCompatActivity {
    Button pausebtn;
    Button leftMove,rightMove, rotateMove, downMove;
    GridView tetrisView,nextTetrisView;
    TextView levelTextView,scoreTextView, maxScoreTextView, speedTextView;

    /** 图形块下落的时间间隔 */
    int timeInterval=800;
    /** 最高成绩 */
    int highestScore=0;
    /** 数据存储信息 */
    CacheUtils cacheUtils;
    /** 游戏等级 */
    int grade;
    /** 游戏是否暂停控制变量 */
    boolean isPause = false;
    /** 得分 */
    int score =0;
    public String TAG = "MainActivity";
    Random random= new Random();
    Timer timer;
    /** 随机图形index，随机颜色index */
    int rand,randColor, nextRand,nextRandColor;
    /** position[1]为y方向位置 */
    int[] position=new int[2];
    int stop = 0;
    /** 方块数为10*15 */
    int xSize=10,ySize=15;
    /** 所有方块用一个数组表示,记录大Grid的每一行 */
    int[] allBlock = new int[ySize];
    /** 记录大Grid的每一个方块的颜色 */
    int[][] blockColor = new int[15][10];
    /** 记录大Grid的每一个方块的颜色，该变量用于向自定义Grid视图传递布局信息 */
    List<Integer> blockList=new ArrayList<>();
    /** 以数字形式存储4×4方格与其中的形状、颜色,存于List中，0代表无block，该变量用于向自定义Grid视图传递布局信息 */
    List<Integer> nextTetrisList=new ArrayList<>();
    BlockAdapter blockAdapter,nextTetrisAdapter;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){    //匿名类
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            boolean isNewTimer=true;

            // 遍历大Grid中的每一个方块
            for (int i=0;i<ySize;i++) {
                if (allBlock[i] == 0) {
                    for (int j = 0; j < xSize; j++) {
                        blockList.set(i * xSize + j, 0);
                    }
                } else {
                    for (int j=0;j<xSize;j++) {
                        blockList.set(i * xSize + j, blockColor[i][j]);
                    }
                }
            }

            if (msg.what == 0) {    //如果处于等时间间隔下落状态
                boolean canMove = true;

                //检测是否可以按要求移动
                position[1]++;
                for(int i = 3; i>=0; i--) {
                    int line = i + position[1];
                    if (line >= 0 && Tetris.shape[rand][i] != 0) {
                        //如果到底了，或者下面有方块
                        if (line >= ySize ||
                                ((allBlock[line] & (leftMath(Tetris.shape[rand][i] ,position[0]))) != 0)) {
                            canMove = false;
                            break;
                        }
                    }
                }
                if (!canMove) {
                    position[1]--;
                    for (int i = 3; i >= 0; i--) {
                        int line = i + position[1];
                        if (line >= 0 && Tetris.shape[rand][i] != 0) {
                            for (int j = 0; j < xSize; j++) {
                                if (((1 << j) & (leftMath(Tetris.shape[rand][i], position[0]))) != 0) {
                                    blockList.set(line * xSize + j, randColor);
                                }
                            }
                        }
                    }
                    stopDown();
                } else {
                    for (int i = 3; i >= 0; i--) {
                        int line = i + position[1];
                        if (line >= 0 && Tetris.shape[rand][i] != 0) {
                            for (int j = 0; j < xSize; j++) {
                                if (((1 << j) & (leftMath(Tetris.shape[rand][i], position[0]))) != 0) {
                                    blockList.set(line * xSize + j, randColor);
                                }
                            }
                        }
                    }
                }
            }else{
                for (int i = 3; i >= 0; i--) {
                    int line = i + position[1];
                    if (line >= 0 && Tetris.shape[rand][i] != 0) {
                        for (int j = 0; j < xSize; j++) {
                            if (((1 << j) & (leftMath(Tetris.shape[rand][i], position[0]))) != 0) {
                                blockList.set(line * xSize + j, randColor);
                            }
                        }
                    }
                }
            }
            blockAdapter.setmDatas(blockList);
            blockAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // 设置格子图形下落间隔
        Intent intent = getIntent();
        this.grade = intent.getIntExtra("grade", 3);
        this.timeInterval=setTimeInterval(this.grade);

        // 获得最高成绩
        cacheUtils = new CacheUtils(MainActivity.this, "UserInfo");
        this.highestScore=getHighestScore(cacheUtils);

        initView();
        initViewListener();

        initTetrisBlock();
        initTetrisGrid();
        initNextTetrisGrid();

        Log.i(TAG, rand + "");

        startTimer();
    }

    /** 根据等级设置相应的格子图形下落时间间隔 */
    private int setTimeInterval(int grade){
        switch (grade) {
            case 1: return 1200;
            case 2: return 1000;
            case 3: return 800;
            case 4: return 600;
            case 5: return 400;
            default: break;
        }
        return timeInterval;
    }

    /** 从存储的数据中获取最高成绩 */
    private int getHighestScore(CacheUtils cacheUtils){
        String maxString = "";
        try {
            maxString = cacheUtils.getValue("highestScore"+grade, String.valueOf(0));
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        try {
            return Integer.parseInt(maxString.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void initView(){
        leftMove = (Button) findViewById(R.id.left_move);
        rightMove = (Button) findViewById(R.id.right_move);
        rotateMove = (Button) findViewById(R.id.rotate_move);
        downMove = (Button) findViewById(R.id.down_move);
        scoreTextView = (TextView) findViewById(R.id.score);
        maxScoreTextView = (TextView) findViewById(maxScore);
        speedTextView = (TextView) findViewById(R.id.speed);
        levelTextView = (TextView) findViewById(R.id.level);
        maxScoreTextView.setText("最高分：" + highestScore);
        scoreTextView.setText("分数："+score);
        levelTextView.setText("等级：" + grade);
        speedTextView.setText("速度：" +1000.0 / timeInterval);
        tetrisView = (GridView) findViewById(R.id.tetrisView);
        nextTetrisView = (GridView) findViewById(R.id.nextTetrisView);
        pausebtn = (Button) findViewById(R.id.pause);
    }

    private void initViewListener(){
        leftMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //左移实际是数字的右移
                //左移需要判断是否能够左移
                for (int i=3;i>=0;i--) {
                    if ((((leftMath(Tetris.shape[rand][i] ,position[0])) >> 1) << 1)
                            != (leftMath(Tetris.shape[rand][i] ,position[0]))) {
                        //如果越界了
                        return;
                    }
                }
                for (int i=3;i>=0;i--) {
                    int line = i + position[1];
                    if (line >= 0 && Tetris.shape[rand][i]!=0) {
                        if ((allBlock[line] & (leftMath(Tetris.shape[rand][i], position[0]) >> 1)) != 0) {
                            return;
                        }
                    }
                }
                position[0]--;
                handler.sendEmptyMessage(1);
            }
        });
        rotateMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int nextRotate = Tetris.nextShape[rand];
                for (int i=3;i>=0;i--) {
                    int line = i + position[1];
                    //检查是否越界
                    if (leftMath(Tetris.shape[nextRotate][i] ,position[0]) > 0x3ff){
                        //右边界
                        return;
                    }else if(Tetris.shape[nextRotate][i]>0 && line>=ySize){
                        //下边界
                        return;
                    } else if (leftMath(leftMath(Tetris.shape[nextRotate][i], position[0]),
                            -position[0]) != Tetris.shape[nextRotate][i]) {
                        return;
                    }
                    //检查是否与其他方块重合
                    else if (line>0 && line<ySize &&
                            (leftMath(Tetris.shape[nextRotate][i], position[0]) & allBlock[line]) != 0) {
                        return;
                    }
                }
                rand = nextRotate;
                handler.sendEmptyMessage(1);
            }
        });
        rightMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i=3;i>=0;i--) {
                    if (((leftMath(Tetris.shape[rand][i], position[0])) << 1) > 0x3ff
                    ) {
                        //如果越界了
                        return;
                    }
                }
                for (int i=3;i>=0;i--) {
                    int line = i + position[1];
                    if (line >= 0 && Tetris.shape[rand][i]!=0) {
                        if ((allBlock[line] & (leftMath(Tetris.shape[rand][i], position[0]) << 1)) != 0) {
                            return;
                        }
                    }
                }
                position[0]++;
                handler.sendEmptyMessage(1);
            }
        });
        downMove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int c;
                int down=1<<10;
                for(int i=3;i>=0;i--) {
                    int line = i + position[1];
                    if (line >= 0 && Tetris.shape[rand][i] != 0) {
                        down = Math.min(down, ySize - line - 1);
                        for (int j=0;j<xSize;j++) {
                            if (((1 << j)& (leftMath(Tetris.shape[rand][i] ,position[0])))!=0) {
                                for(int k=0;k+line<ySize;k++) {
                                    if (blockColor[k + line][j] > 0) {
                                        down = Math.min(down, k-1);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (down <= 0 || down==(1<<10)) {
                    return;
                } else {
                    position[1] += down;
                    handler.sendEmptyMessage(0);
                }
            }
        });
        pausebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });
    }

    /** 初始化第一个和下一个俄罗斯小方块 */
    private void initTetrisBlock(){
        //初始化第一个俄罗斯小方块
        rand = random.nextInt(19);
        position[0] = Tetris.initPosition[rand][0];
        position[1] = Tetris.initPosition[rand][1];
        randColor = random.nextInt(5) + 1;

        //设置下一个方块
        nextRand = random.nextInt(19);
        nextRandColor = random.nextInt(5) + 1;  //随机选择一个小方块.png
    }

    /** 初始化空白游戏格子 */
    private void initTetrisGrid(){
        for(int i=0;i<10;i++){
            for(int j=0;j<15;j++){
                blockList.add(0);
            }
        }
        //绑定数据和自定义视图
        blockAdapter = new BlockAdapter(MainActivity.this, blockList, R.layout.item_adapter);
        tetrisView.setAdapter(blockAdapter);
    }

    /** 初始化并显示下一个方块的GridView */
    private void initNextTetrisGrid(){
        nextTetrisList.clear();
        //找到4×4方格中像素值为1的点并存储
        for (int i=0;i<4;i++) {
            for (int j=0;j<4;j++) {
                if (((1 << j) & Tetris.shape[nextRand][i]) != 0) {
                    nextTetrisList.add(nextRandColor);
                } else {
                    nextTetrisList.add(0);
                }
            }
        }
        //设置View
        nextTetrisAdapter = new BlockAdapter(MainActivity.this, nextTetrisList, R.layout.item_adapter);
        nextTetrisView.setAdapter(nextTetrisAdapter); //显示
    }

    private void nextTetrisShow() {
        nextTetrisList.clear();
        for (int i=0;i<4;i++) {
            for (int j=0;j<4;j++) {
                if (((1 << j)& Tetris.shape[nextRand][i])!=0) {
                    nextTetrisList.add(nextRandColor);
                } else {
                    nextTetrisList.add(0);
                }
            }
        }
        nextTetrisAdapter.setmDatas(nextTetrisList);
        nextTetrisAdapter.notifyDataSetChanged();
    }

    /** 继续、暂停游戏控制方法 */
    private void pause() {
        isPause = !isPause;
        if (isPause) {
            stopTimer();
            pausebtn.setText("继续");
            leftMove.setEnabled(false);
            rightMove.setEnabled(false);
            rotateMove.setEnabled(false);
            downMove.setEnabled(false);
        } else {
            startTimer();
            pausebtn.setText("暂停");
            leftMove.setEnabled(true);
            rightMove.setEnabled(true);
            rotateMove.setEnabled(true);
            downMove.setEnabled(true);
        }
    }

    /** 启动计时器 */
    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
        }
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(0);
            }
        },0,timeInterval);
    }

    /** 停止计时器 */
    private void stopTimer(){
        if(timer != null){
            timer.cancel();
            // 一定设置为null，否则定时器不会被回收
            timer = null;
        }
    }

    int leftMath(int a, int b) {
        if (b < 0) {
            return a >> -b;
        } else {
            return a << b;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }

    /** 写入、消除、重置 */
    void stopDown() {
        for(int i=3;i>=0;i--) {
            int line = i + position[1];
            if (line >= 0 && Tetris.shape[rand][i] != 0) {
                allBlock[line] += (leftMath(Tetris.shape[rand][i] ,position[0]));
                for (int j=0;j<xSize;j++) {
                    if (((1 << j)& (leftMath(Tetris.shape[rand][i] ,position[0])))!=0) {
                        blockColor[line][j] = randColor;
                    }
                }
            }
        }
        for(int i=ySize-1;i>=0;) {
            if (allBlock[i] == 0x3ff) { //如果第i+1行被填满，上层方块下移
                score++;
                scoreTextView.setText("分数："+score);
                for (int j = i - 1; j >= 0; j--) {
                    allBlock[j + 1] = allBlock[j];  //上一行下移一行
                    for(int k=0;k<xSize;k++) {
                        blockColor[j + 1][k] = blockColor[j][k];    // 上一行对应的颜色也移至下一行
                    }
                }
                allBlock[0] = 0;
                for (int j=0;j<xSize;j++) {
                    blockColor[0][j]=0;
                }
            } else {
                i--;
            }
        }
        if (allBlock[0] != 0) { //如果最顶层被填满，游戏结束
            if (score > highestScore) {
                cacheUtils.getValue("highestScore"+grade, score +"");
                highestScore = score;
                maxScoreTextView.setText("最高分：" + highestScore);
                scoreTextView.setText("分数："+score);
            }
            gameOver();
        }
        initTetrisBlock();
        nextTetrisShow();
        Log.i(TAG, rand + "");
    }

    /** 游戏结束 */
    private void gameOver() {
        cacheUtils.putValue("highestScore"+grade, String.valueOf(highestScore));
        stopTimer();

        // 构造一个对话框
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle("游戏结束");
        dialog.setMessage("本局您获得" + score + "分");
        dialog.setPositiveButton("再来一局", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stop = 0;
                for (int i=0;i<ySize;i++) {
                    allBlock[i] = 0;
                    for (int j=0;j<xSize;j++) {
                        blockColor[i][j]=0;
                    }
                }
                initTetrisBlock();
                startTimer();
            }
        });
        dialog.setNegativeButton("退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        }).create();
        dialog.setCancelable(false);
        dialog.show();
    }
}