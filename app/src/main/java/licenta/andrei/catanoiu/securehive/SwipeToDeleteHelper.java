package licenta.andrei.catanoiu.securehive;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

public abstract class SwipeToDeleteHelper {
    private ListView listView;
    private float downX;
    private float upX;
    private int swipedPosition;
    private static final float SWIPE_MIN_DISTANCE = 120;

    public SwipeToDeleteHelper(ListView listView) {
        this.listView = listView;
        this.listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        swipedPosition = listView.pointToPosition((int) event.getX(), (int) event.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                        upX = event.getX();
                        float deltaX = downX - upX;
                        if (Math.abs(deltaX) > SWIPE_MIN_DISTANCE && deltaX > 0) {
                            // Swipe la stânga
                            onSwipeLeft(swipedPosition);
                            return true;
                        }
                        break;
                }
                return false;
            }
        });
    }

    public abstract void onSwipeLeft(int position);
}