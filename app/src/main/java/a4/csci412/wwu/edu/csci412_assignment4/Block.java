package a4.csci412.wwu.edu.csci412_assignment4;

import android.graphics.Rect;

public class Block {
    private int x, y;
    private Rect rect;
    private boolean alive;

    public Block(int x, int y, int w, int h) {
        rect = new Rect(x, y, x + w, y + h);
        alive = true;
    }

    public boolean isAlive() {
        return alive;
    }

    public Rect getRect() {
        return rect;
    }

    public void destroy() {
        alive = false;
    }
}
