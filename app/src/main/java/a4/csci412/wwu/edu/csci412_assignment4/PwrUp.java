package a4.csci412.wwu.edu.csci412_assignment4;

import android.graphics.Rect;

public class PwrUp {
    private int x, y, yVel;
    private Rect rect;
    private boolean alive;

    public PwrUp() {
        alive = false;
    }

    public void drop(int x, int y) {
        alive = true;
        rect = new Rect(x, y, x + 20, y + 20);
        yVel = 4;
    }
    public void grab() {
        alive = false;
    }
    public boolean isAlive() {
        return alive;
    }
    public Rect getRect() {
        return rect;
    }
}