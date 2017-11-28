package a4.csci412.wwu.edu.csci412_assignment4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class Game extends View {

    private final int FPS = 30;
    private final int DOUBLETAP_BUFFER = 200;

    private int moveSpeed = 10;
    private int airdashSpeed = 50;
    private int airdashFrames = 5;

    private int batY = 1000;
    private int batW = 100;
    private int ballSize = 40;
    private int blockW = 120;
    private int blockH = 50;

    private boolean running;
    private int width, height;
    private Paint black, orange, purple, green, red;
    private Rect bat, ball, ai1, ai2, ai3;
    private int batVel;
    private int ballXVel, ballYVel;
    private Timer clock = new Timer();
    private Timer tapClock = new Timer();
    private Timer OD = new Timer();
    private int ODtime = 5000;
    private boolean ODactive = false;
    private int tap;
    private int airdash = 0;
    private Random rng;
    private boolean clear;
    private int score, lives;
    private LinkedList<Integer> history;

    private Set<Block> blocks;
    private Set<PwrUp> pickups;

    private MediaPlayer hit1;
    private MediaPlayer hit2;

    public Game(Context c) {
        super(c);
        black = new Paint();
        black.setColor(0xFF000000);
        orange = new Paint();
        orange.setColor(0xFFE59400);
        purple = new Paint();
        purple.setColor(0xFF551A8B);
        green = new Paint();
        green.setColor(0xFF00DD00);
        red = new Paint();
        red.setColor(0xFFEE0000);
        hit1 = MediaPlayer.create(c, R.raw.hit1);
        hit2 = MediaPlayer.create(c, R.raw.hit2);
        start();
    }

    public void start() {
        bat = new Rect(250, batY, 250 + batW, batY + 40);
        ball = new Rect(60, 800, 60 + ballSize, 800 + ballSize);
        ballXVel = 15;
        ballYVel = -13;
        blocks = new HashSet<>();
        rng = new Random();
        pickups = new HashSet<>();
        score = 0;
        lives = 3;
        ai1 = new Rect();
        ai2 = new Rect();
        ai3 = new Rect();
        history = new LinkedList<>();

        // quick and dirty generate semi random grid
        int r = rng.nextInt(6);
        for (int i = 1; i < 8; i++) {
            blocks.add(new Block(i * (blockW + r), 100, blockW, blockH));
            blocks.add(new Block(i * (blockW + r), 340, blockW, blockH));
        }
        for (int i = 2; i < 7; i++) {
            blocks.add(new Block(i * (blockW + r + 2), 200, blockW, blockH));
            blocks.add(new Block(i * (blockW + r + 8), 400, blockW, blockH));
        }
        for (int i = 0; i < 8; i++) {
            blocks.add(new Block(i * (blockW + r) + 40, 260, blockW, blockH));
        }

        running = true;
        clear = false;
        clock.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (running) update();
            }
        }, 0, 1000 / FPS);
    }

    // logic loop
    public void update() {
        if (width == 0 || height == 0) {
            width = getWidth();
            height = getHeight();
        }

        // store 30 frames of bat position history
        history.add(bat.left);
        if (history.size() > 30) history.remove();

        if (airdash > 0) {
            bat.offset(airdashSpeed, 0);
            airdash--;
        } else if (airdash < 0) {
            bat.offset(-airdashSpeed, 0);
            airdash++;
        } else {
            bat.offset(batVel, 0);
        }
        if (bat.left < 0) bat.offset(-bat.left, 0);
        else if (bat.right > width) bat.offset(width - bat.right, 0);

        collisions();

        if (ball.centerX() <= 0 || ball.centerX() >= width) ballXVel = -ballXVel;
        if (ball.centerY() <= 0) ballYVel = -ballYVel;
        if (ball.centerY() > bat.centerY() + 200) {
            lives--;
            Log.i("state", "life lost, remaining: " + lives);
            ballYVel = -ballYVel;
            if (lives == 0) lose();
        }
        ball.offset(ballXVel, ballYVel);

        invalidate();
    }

    public void collisions() {
        if (Rect.intersects(bat, ball) && ballYVel > 0) {
            hit2.start();
            ballYVel = -ballYVel;
            if (batVel != 0) ballXVel = batVel;
            if (airdash != 0) ballXVel *= 2;
        }

        // pickup grants overdrive and/or bonus points
        for (PwrUp p : pickups) {
            if (p.isAlive()) {
                p.getRect().offset(0, 6);
                if (Rect.intersects(bat, p.getRect())) {
                    score += 2;
                    p.grab();
                    if (!ODactive) overdrive();
                    else score += 3;
                }
            }
        }

        clear = true;
        for (Block b : blocks) {
            if (b.isAlive()) {
                clear = false;
                if (Rect.intersects(b.getRect(), ball)) {
                    hit1.start();
                    b.destroy();
                    score++;
                    if (ball.left < b.getRect().right && ball.right > b.getRect().left)
                        ballYVel = -ballYVel;
                    else ballXVel = -ballXVel;
                    // roll for pickup, 1/4 chance
                    int r = rng.nextInt(3);
                    if (r == 0) {
                        PwrUp p = new PwrUp();
                        p.drop(b.getRect().centerX(), b.getRect().centerY());
                        pickups.add(p);
                    }
                }
            }
        }
        if (clear) win();
    }

    public void win() {
        Log.i("state", "won game");
        running = false;
    }

    public void lose() {
        Log.i("state", "lost game");
        running = false;
    }

    // temporary and permanent power boost
    public void overdrive() {
        ODactive = true;
        moveSpeed += 5;
        airdashSpeed += 5;
        bat.right += 40;
        bat.left -= 40;
        OD.schedule(new TimerTask() {
            @Override
            public void run() {
                ODactive = false;
                moveSpeed -= 4;
                airdashSpeed -= 4;
                bat.right -= 30;
                bat.left += 30;
            }
        }, ODtime);
    }

    public void onDraw(Canvas canvas) {
        canvas.drawRect(ball, orange);
        for (Block b : blocks) {
            if (b.isAlive()) canvas.drawRect(b.getRect(), purple);
        }
        for (PwrUp p : pickups) {
            if (p.isAlive()) canvas.drawRect(p.getRect(), green);
        }

        // draw afterimages based on bat position history
        int i = history.size();
        if (ODactive && i > 9) {
            ai1.set(bat.left, bat.top, bat.right, bat.bottom);
            ai2.set(bat.left, bat.top, bat.right, bat.bottom);
            ai3.set(bat.left, bat.top, bat.right, bat.bottom);
            ai1.offsetTo(history.get(i - 3), bat.top);
            ai2.offsetTo(history.get(i - 6), bat.top);
            ai3.offsetTo(history.get(i - 9), bat.top);

            red.setAlpha(0x99);
            canvas.drawRect(ai1, red);
            red.setAlpha(0x66);
            canvas.drawRect(ai2, red);
            red.setAlpha(0x44);
            canvas.drawRect(ai3, red);
            red.setAlpha(0xFF);
            canvas.drawRect(bat, red);
        } else canvas.drawRect(bat, black);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                tap++;
                if (tap > 1) {
                    if (x > width / 2) airdash = airdashFrames;
                    else airdash = -airdashFrames;
                    tap = 0;
                }
                tapClock.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        tap = 0;
                    }
                }, DOUBLETAP_BUFFER);
                if (x > width / 2) batVel = moveSpeed;
                else batVel = -moveSpeed;
                break;
            case MotionEvent.ACTION_UP:
                batVel = 0;
                break;
        }
        return true;
    }

    // game state info for use outside fragment
    public void pause() {
        running = false;
    }
    public void resume() {
        running = true;
    }
    public int getScore() {
        return score;
    }
    public int getState() {
        if (running) return 0;
        else if (clear) return 1;
        else if (lives == 0) return 2;
        else return -1;
    }
}