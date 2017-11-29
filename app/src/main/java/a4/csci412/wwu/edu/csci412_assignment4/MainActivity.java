package a4.csci412.wwu.edu.csci412_assignment4;

import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends FragmentActivity
        implements GameScreen.OnFragmentInteractionListener,
            GameMenu.OnFragmentInteractionListener,
            GameDisplay.OnFragmentInteractionListener {
    private final int FPS = 30;

    GameScreen gs;
    Game game;
    GameDisplay display;
    Timer update;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gs = (GameScreen) getSupportFragmentManager().findFragmentById(R.id.game);
        game = (Game) gs.getView();
        display = (GameDisplay) getSupportFragmentManager().findFragmentById(R.id.display);
        update = new Timer();
        update.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateScore();
            }
        }, 0, 1000 / FPS);
    }

    public void buttonClick(View v) {
        Button b = (Button) v;
        switch (b.getId()) {
            case R.id.start:
                if (game.getState() > 0) {
                    game.start();
                    game.resume();
                } else if (game.getState() < 0) {
                    game.resume();
                }
                break;
            case R.id.pause:
                game.pause();
                break;
            case R.id.stop:
                game.stop();
                break;
        }
    }

    public void updateScore() {
        display.setScore("Score: " + game.getScore());
        display.setLives("Lives: " + game.getLives());
        switch (game.getState()) {
            case 0:
                display.setState("");
                break;
            case 1:
                display.setState("Game won.");
                break;
            case 2:
                display.setState("Game lost.");
                break;
            case -1:
                display.setState("(paused)");
        }
    }
}
