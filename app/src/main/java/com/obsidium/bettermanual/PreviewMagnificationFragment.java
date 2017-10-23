package com.obsidium.bettermanual;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import com.obsidium.bettermanual.camera.CameraInstance;
import com.obsidium.bettermanual.views.FocusScaleView;
import com.obsidium.bettermanual.views.PreviewNavView;
import com.obsidium.bettermanual.views.StarDriftAlginView;
import com.sony.scalar.hardware.CameraEx;


public class PreviewMagnificationFragment extends BaseLayout implements KeyEvents, CameraEx.PreviewMagnificationListener, CameraEx.FocusDriveListener {


    private final float STEP_MAG_SIZE = 100f;

    @Override
    public void onChanged(CameraEx.FocusPosition focusPosition, CameraEx cameraEx) {
        m_lFocusScale.setVisibility(View.VISIBLE);
        m_focusScaleView.setMaxPosition(focusPosition.maxPosition);
        m_focusScaleView.setCurPosition(focusPosition.currentPosition);
        activityInterface.getMainHandler().removeCallbacks(m_hideFocusScaleRunnable);
        activityInterface.getMainHandler().postDelayed(m_hideFocusScaleRunnable, 2000);
    }


    private final Runnable m_hideFocusScaleRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            m_lFocusScale.setVisibility(View.GONE);
        }
    };

    private class SurfaceSwipeTouchListener extends OnSwipeTouchListener
    {
        public SurfaceSwipeTouchListener(Context context)
        {
            super(context);
        }

        @Override
        public boolean onScrolled(float distanceX, float distanceY)
        {
            if (m_curPreviewMagnification != 0)
            {
                m_curPreviewMagnificationPos = new Pair<Integer, Integer>(Math.max(Math.min(m_curPreviewMagnificationMaxPos, m_curPreviewMagnificationPos.first + (int)distanceX), -m_curPreviewMagnificationMaxPos),
                        Math.max(Math.min(m_curPreviewMagnificationMaxPos, m_curPreviewMagnificationPos.second + (int)distanceY), -m_curPreviewMagnificationMaxPos));
                CameraInstance.GET().setPreviewMagnification(m_curPreviewMagnification, m_curPreviewMagnificationPos);
                return true;
            }
            return false;
        }
    }

    private final String TAG = PreviewMagnificationFragment.class.getSimpleName();

    private TextView magnification;
    private PreviewNavView previewNavView;

    // Preview magnification
    private boolean         m_zoomLeverPressed;
    private int             m_curPreviewMagnification;
    private float           m_curPreviewMagnificationFactor;
    private Pair<Integer, Integer> m_curPreviewMagnificationPos = new Pair<Integer, Integer>(0, 0);
    private int             m_curPreviewMagnificationMaxPos;
    private FocusScaleView m_focusScaleView;
    private View            m_lFocusScale;

    private StarDriftAlginView starDriftAlginView;

    public PreviewMagnificationFragment(Context context, ActivityInterface activityInterface) {
        super(context, activityInterface);
        inflateLayout(R.layout.preview_magnification_fragment);
        magnification = (TextView) findViewById(R.id.tvMagnification);
        previewNavView = (PreviewNavView)findViewById(R.id.vPreviewNav);
        starDriftAlginView = (StarDriftAlginView)findViewById(R.id.stardriftalgin);
        m_focusScaleView = (FocusScaleView)findViewById(R.id.vFocusScale);

        m_lFocusScale = findViewById(R.id.lFocusScale);
        m_lFocusScale.setVisibility(View.GONE);
        if (!activityInterface.getPreferences().showStarAlginView())
            starDriftAlginView.setVisibility(GONE);
        else
            starDriftAlginView.enableGrid(activityInterface.getPreferences().showStarAlginViewGrid());

        activityInterface.setSurfaceViewOnTouchListner(new SurfaceSwipeTouchListener(getContext()));
        CameraInstance.GET().setPreviewMagnificationListener(this);
        m_curPreviewMagnification = 100;
        CameraInstance.GET().setPreviewMagnification(m_curPreviewMagnification, m_curPreviewMagnificationPos);
        CameraInstance.GET().setFocusDriveListener(this);
    }

    @Override
    public void Destroy() {
        activityInterface.setSurfaceViewOnTouchListner(null);
    }

    //##############CameraEx.PreviewMagnificationListener###############
    @Override
    public void onChanged(boolean enabled, int magFactor, int magLevel, Pair coords, CameraEx cameraEx) {
        Log.d(TAG,"onChanged enabled:" + String.valueOf(enabled) + " magFactor:" + String.valueOf(magFactor) + " magLevel:" +
                String.valueOf(magLevel) + " x:" + coords.first + " y:" + coords.second + "\n");
        //*
        if (enabled)
        {
            //log("m_curPreviewMagnificationMaxPos: " + String.valueOf(m_curPreviewMagnificationMaxPos) + "\n");
            m_curPreviewMagnification = magLevel;
            m_curPreviewMagnificationFactor = ((float)magFactor / 100.0f);
            m_curPreviewMagnificationMaxPos = 1000 - (int)(1000.0f / m_curPreviewMagnificationFactor);
            magnification.setText(String.format("\uE012 %.2fx", (float)magFactor / 100.0f));
            previewNavView.update(coords, m_curPreviewMagnificationFactor);
        }
        else
        {
            previewNavView.update(null, 0);
            m_curPreviewMagnification = 0;
            m_curPreviewMagnificationMaxPos = 0;
            m_curPreviewMagnificationFactor = 0;
        }
    }

    @Override
    public void onInfoUpdated(boolean b, Pair pair, CameraEx cameraEx) {

    }

    private void movePreviewVertical(int delta)
    {
        int newY = m_curPreviewMagnificationPos.second + delta;
        if (newY > m_curPreviewMagnificationMaxPos)
            newY = m_curPreviewMagnificationMaxPos;
        else if (newY < -m_curPreviewMagnificationMaxPos)
            newY = -m_curPreviewMagnificationMaxPos;
        m_curPreviewMagnificationPos = new Pair<Integer, Integer>(m_curPreviewMagnificationPos.first, newY);
        CameraInstance.GET().setPreviewMagnification(m_curPreviewMagnification, m_curPreviewMagnificationPos);
    }

    private void movePreviewHorizontal(int delta)
    {
        int newX = m_curPreviewMagnificationPos.first + delta;
        if (newX > m_curPreviewMagnificationMaxPos)
            newX = m_curPreviewMagnificationMaxPos;
        else if (newX < -m_curPreviewMagnificationMaxPos)
            newX = -m_curPreviewMagnificationMaxPos;
        m_curPreviewMagnificationPos = new Pair<Integer, Integer>(newX, m_curPreviewMagnificationPos.second);
        CameraInstance.GET().setPreviewMagnification(m_curPreviewMagnification, m_curPreviewMagnificationPos);
    }

    @Override
    public boolean onUpperDialChanged(int value) {
        return false;
    }

    @Override
    public boolean onLowerDialChanged(int value) {
        if (starDriftAlginView.getVisibility() == VISIBLE) {
            if (value < 0)
                starDriftAlginView.decraseSize();
            else
                starDriftAlginView.increaseSize();
            starDriftAlginView.invalidate();
        }

        return false;
    }

    @Override
    public boolean onUpKeyDown() {
        if (m_curPreviewMagnification != 0)
        {
            movePreviewVertical((int)(-STEP_MAG_SIZE / m_curPreviewMagnificationFactor));
            return true;
        }
        return false;
    }

    @Override
    public boolean onUpKeyUp() {

        return false;
    }

    @Override
    public boolean onDownKeyDown() {
        if (m_curPreviewMagnification != 0) {
            movePreviewVertical((int) (STEP_MAG_SIZE / m_curPreviewMagnificationFactor));
            return true;
        }
        return false;
    }

    @Override
    public boolean onDownKeyUp() {

        return false;
    }

    @Override
    public boolean onLeftKeyDown() {
        if (m_curPreviewMagnification != 0)
        {
            movePreviewHorizontal((int)(-STEP_MAG_SIZE / m_curPreviewMagnificationFactor));
            return true;
        }
        return false;
    }

    @Override
    public boolean onLeftKeyUp() {
        return false;
    }

    @Override
    public boolean onRightKeyDown() {
        if (m_curPreviewMagnification != 0)
        {
            movePreviewHorizontal((int)(STEP_MAG_SIZE / m_curPreviewMagnificationFactor));
            return true;
        }
        return false;
    }

    @Override
    public boolean onRightKeyUp() {
        return false;
    }

    @Override
    public boolean onEnterKeyDown() {
        return false;
    }

    @Override
    public boolean onEnterKeyUp() {
        /*if (m_curPreviewMagnification == 200)
        {
            m_curPreviewMagnification = 0;
            m_curPreviewMagnificationPos = new Pair<Integer, Integer>(0, 0);
        }
        else*/ if (m_curPreviewMagnification == 200)
        {
            m_curPreviewMagnification = 100;
        }
        else if (m_curPreviewMagnification == 100)
            m_curPreviewMagnification = 200;
        CameraInstance.GET().setPreviewMagnification(m_curPreviewMagnification, m_curPreviewMagnificationPos);
        return false;
    }

    @Override
    public boolean onFnKeyDown() {
        return false;
    }

    @Override
    public boolean onFnKeyUp() {

        if (starDriftAlginView.getVisibility() == VISIBLE) {
            if (activityInterface.getPreferences().showStarAlginViewGrid()) {
                activityInterface.getPreferences().setShowStarAlignGrid(false);
                starDriftAlginView.enableGrid(false);
            }
            else {
                activityInterface.getPreferences().setShowStarAlignGrid(true);
                starDriftAlginView.enableGrid(true);
            }
        }
        return false;
    }

    @Override
    public boolean onAelKeyDown() {
        return false;
    }

    @Override
    public boolean onAelKeyUp() {
        activityInterface.getDialHandler().setDefaultListner();
        CameraInstance.GET().stopPreviewMagnification();
        activityInterface.loadFragment(MainActivity.FRAGMENT_CAMERA_UI);
        return true;
    }

    @Override
    public boolean onMenuKeyDown() {
        return false;
    }

    @Override
    public boolean onMenuKeyUp() {
        if (starDriftAlginView.getVisibility() == GONE) {
            starDriftAlginView.setVisibility(VISIBLE);
            activityInterface.getPreferences().setShowStarAlign(true);
        }
        else {
            starDriftAlginView.setVisibility(GONE);
            activityInterface.getPreferences().setShowStarAlign(false);
        }
        return false;
    }

    @Override
    public boolean onFocusKeyDown() {
        return false;
    }

    @Override
    public boolean onFocusKeyUp() {
        return false;
    }

    @Override
    public boolean onShutterKeyDown() {
        return false;
    }

    @Override
    public boolean onShutterKeyUp() {
        return false;
    }

    @Override
    public boolean onPlayKeyDown() {
        return false;
    }

    @Override
    public boolean onPlayKeyUp() {
        return false;
    }

    @Override
    public boolean onMovieKeyDown() {
        return false;
    }

    @Override
    public boolean onMovieKeyUp() {
        return false;
    }

    @Override
    public boolean onC1KeyDown() {
        return false;
    }

    @Override
    public boolean onC1KeyUp() {
        return false;
    }

    @Override
    public boolean onLensAttached() {
        return false;
    }

    @Override
    public boolean onLensDetached() {
        return false;
    }

    @Override
    public boolean onModeDialChanged(int value) {
        return false;
    }

    @Override
    public boolean onZoomTeleKey() {
        return false;
    }

    @Override
    public boolean onZoomWideKey() {
        return false;
    }

    @Override
    public boolean onZoomOffKey() {
        return false;
    }

    @Override
    public boolean onDeleteKeyDown() {
        return false;
    }

    @Override
    public boolean onDeleteKeyUp() {
        return false;
    }
}
