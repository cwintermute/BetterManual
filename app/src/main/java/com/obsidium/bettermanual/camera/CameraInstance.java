package com.obsidium.bettermanual.camera;

import android.hardware.Camera;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;

import com.obsidium.bettermanual.Preferences;
import com.obsidium.bettermanual.controller.ApertureController;
import com.obsidium.bettermanual.controller.DriveModeController;
import com.obsidium.bettermanual.controller.ExposureCompensationController;
import com.obsidium.bettermanual.controller.ExposureHintController;
import com.obsidium.bettermanual.controller.ExposureModeController;
import com.obsidium.bettermanual.controller.FocusDriveController;
import com.obsidium.bettermanual.controller.HistogramController;
import com.obsidium.bettermanual.controller.ImageStabilisationController;
import com.obsidium.bettermanual.controller.IsoController;
import com.obsidium.bettermanual.controller.LongExposureNoiseReductionController;
import com.obsidium.bettermanual.controller.ShutterController;
import com.obsidium.bettermanual.model.ApertureModel;
import com.obsidium.bettermanual.model.DriveModeModel;
import com.obsidium.bettermanual.model.ExposureCompensationModel;
import com.obsidium.bettermanual.model.ExposureHintModel;
import com.obsidium.bettermanual.model.ExposureModeModel;
import com.obsidium.bettermanual.model.FocusDriveModel;
import com.obsidium.bettermanual.model.HistogramModel;
import com.obsidium.bettermanual.model.ImageStabilisationModel;
import com.obsidium.bettermanual.model.IsoModel;
import com.obsidium.bettermanual.model.LongExposureNoiseReductionModel;
import com.obsidium.bettermanual.model.ShutterModel;
import com.sony.scalar.hardware.CameraEx;
import com.sony.scalar.hardware.CameraSequence;

import java.util.List;
import java.util.StringTokenizer;


public class CameraInstance extends BaseCamera implements  CameraSequence.ShutterSequenceCallback   {
    private final String TAG = CameraInstance.class.getSimpleName();

//    public CameraSequence cameraSequence;

    final int SETSURFACE = 0;
    private static CameraInstance INSTANCE = new CameraInstance();
    private SurfaceHolder surfaceHolder;
    private ApertureModel apertureModel;
    private ShutterModel shutterModel;
    private IsoModel isoModel;
    private ExposureCompensationModel exposureCompensationModel;
    private ExposureHintModel exposureHintModel;
    private ExposureModeModel exposureModeModel;
    private DriveModeModel driveModeModel;
    private ImageStabilisationModel imageStabilisationModel;
    private LongExposureNoiseReductionModel longExposureNoiseReductionModel;
    private HistogramModel histogramModel;
    private FocusDriveModel focusDriveModel;


    private CameraInstance() {
        super();
    }

    public static CameraInstance GET()
    {
        return INSTANCE;
    }

    public void initHandler(Looper looper)
    {
        cameraHandler = new CameraHandler(looper,this);
    }


    public void startCamera() {
/*        CameraEx.OpenOptions options = new CameraEx.OpenOptions();
        options.setPreview(true);*/
        Log.d(TAG, "Open Cam");
        m_camera = CameraEx.open(0, null);
        cameraIsOpen = true;
        /*cameraSequence = CameraSequence.open(m_camera);
        setOptions(null);
        cameraSequence.setShutterSequenceCallback(this);*/
        Log.d(TAG, "Cam open");
        cameraHandler.sendMessage(cameraHandler.obtainMessage(MSG_INIT_CAMERA));
        fireOnCameraOpen(true);

    }

    public void initParameters()
    {
        setFocusMode(CameraEx.ParametersModifier.FOCUS_MODE_MANUAL);
        final String sceneMode = Preferences.GET().getSceneMode();
        setSceneMode(sceneMode);
        setDriveMode(Preferences.GET().getDriveMode());
        setBurstDriveSpeed(Preferences.GET().getBurstDriveSpeed());
        // Minimum shutter speed
        if(isAutoShutterSpeedLowLimitSupported()) {
            if (sceneMode.equals(CameraEx.ParametersModifier.SCENE_MODE_MANUAL_EXPOSURE))
                setAutoShutterSpeedLowLimit(-1);
            else
                setAutoShutterSpeedLowLimit(Preferences.GET().getMinShutterSpeed());
        }
        // Disable self timer
        setSelfTimer(0);
        // Force aspect ratio to 3:2
        setImageAspectRatio(CameraEx.ParametersModifier.IMAGE_ASPECT_RATIO_3_2);
        setImageQuality(CameraEx.ParametersModifier.PICTURE_STORAGE_FMT_RAW);


        apertureModel = new ApertureModel(this);
        m_camera.setApertureChangeListener(apertureModel);
        ApertureController.GetInstance().bindModel(apertureModel);

        shutterModel = new ShutterModel(this);
        m_camera.setShutterSpeedChangeListener(shutterModel);
        ShutterController.GetInstance().bindModel(shutterModel);

        isoModel = new IsoModel(this);
        m_camera.setAutoISOSensitivityListener(isoModel);
        IsoController.GetInstance().bindModel(isoModel);

        exposureCompensationModel = new ExposureCompensationModel(this);
        ExposureCompensationController.GetInstance().bindModel(exposureCompensationModel);

        exposureHintModel = new ExposureHintModel(this);
        m_camera.setProgramLineRangeOverListener(exposureHintModel);
        ExposureHintController.GetInstance().bindModel(exposureHintModel);

        exposureModeModel = new ExposureModeModel(this);
        ExposureModeController.GetInstance().bindModel(exposureModeModel);

        driveModeModel = new DriveModeModel(this);
        DriveModeController.GetInstance().bindModel(driveModeModel);

        if (isImageStabSupported()) {
            imageStabilisationModel = new ImageStabilisationModel(this);
            ImageStabilisationController.GetInstance().bindModel(imageStabilisationModel);
        }

        if (isLongExposureNoiseReductionSupported())
        {
            longExposureNoiseReductionModel = new LongExposureNoiseReductionModel(this);
            LongExposureNoiseReductionController.GetIntance().bindModel(longExposureNoiseReductionModel);
        }

        focusDriveModel = new FocusDriveModel(this);
        m_camera.setFocusDriveListener(focusDriveModel);
        FocusDriveController.GetInstance().bindModel(focusDriveModel);

        histogramModel = new HistogramModel(this);
        m_camera.setPreviewAnalizeListener(histogramModel);
        HistogramController.GetInstance().bindModel(histogramModel);


        //dumpParameter();
    }

    public void closeCamera() {
        cameraIsOpen = false;
        Log.d(TAG, "closeCamera");

        Preferences.GET().setSceneMode(exposureModeModel.getStringValue());
        // Drive mode and burst speed
        Preferences.GET().setDriveMode(driveModeModel.getValue());
        Preferences.GET().setBurstDriveSpeed(getBurstDriveSpeed());

        ApertureController.GetInstance().bindModel(null);
        m_camera.setApertureChangeListener(null);
        apertureModel = null;

        ShutterController.GetInstance().bindModel(null);
        m_camera.setShutterSpeedChangeListener(null);
        shutterModel = null;

        IsoController.GetInstance().bindModel(null);
        m_camera.setAutoISOSensitivityListener(null);
        isoModel = null;

        ExposureCompensationController.GetInstance().bindModel(null);
        exposureCompensationModel = null;

        ExposureHintController.GetInstance().bindModel(null);
        exposureHintModel = null;

        ExposureModeController.GetInstance().bindModel(null);
        exposureModeModel = null;

        DriveModeController.GetInstance().bindModel(null);
        driveModeModel = null;

        HistogramController.GetInstance().bindModel(null);
        m_camera.setPreviewAnalizeListener(null);
        histogramModel = null;

        FocusDriveController.GetInstance().bindModel(null);
        m_camera.setFocusDriveListener(null);
        focusDriveModel = null;

        /*cameraSequence.setShutterSequenceCallback(null);
        cameraSequence.release();*/
        m_camera.getNormalCamera().stopPreview();
        m_camera.getNormalCamera().release();
        m_camera.release();
        m_camera = null;
        surfaceHolder = null;
    }

    public void setSurfaceHolder(SurfaceHolder surface) {
        this.surfaceHolder = surface;
    }

    public void startPreview() {
        cameraHandler.sendMessage(cameraHandler.obtainMessage(START_PREVIEW));
    }

    public void stopPreview() {
        cameraHandler.sendMessage(cameraHandler.obtainMessage(STOP_PREVIEW));
    }

    public void enableHwShutterButton() {
        m_camera.startDirectShutter();

    }

    public void disableHwShutterButton() {
        m_camera.stopDirectShutter(null);
    }

    public void cancelCapture()
    {
        cameraHandler.sendMessage(cameraHandler.obtainMessage(CANCEL_CAPTURE));
    }

    public void takePicture()
    {
        cameraHandler.sendMessage(cameraHandler.obtainMessage(CAPTURE_IMAGE));

    }

    @Override
    public void onShutterSequence(CameraSequence.RawData rawData, CameraSequence cameraSequence) {
        Log.d(TAG,"onShutterSequence");
        m_camera.cancelTakePicture();
        //cameraSequence.setReleaseLock(true);
    }

  /*  @Override
    public void onSplitShutterSequence(CameraSequence.RawData rawData, CameraSequence.SplitExposureProgressInfo splitExposureProgressInfo, CameraSequence cameraSequence) {
        Log.d(TAG, "onSplitShutterSequence();");
        cameraSequence.setReleaseLock(false);
    }

    @Override
    public void onShutterSequence(CameraSequence.RawData rawData, CameraSequence cameraSequence) {
        Log.d(TAG,"onShutterSequence");
        cameraSequence.setReleaseLock(false);
    }*/


    public void initCamera()
    {
        autoPictureReviewControl = new CameraEx.AutoPictureReviewControl();
        m_camera.setAutoPictureReviewControl(getAutoPictureReviewControls());
        autoPictureReviewControl.setPictureReviewTime(0);
        autoPictureReviewControl.cancelAutoPictureReview();

        initParameters();

        //when false cameraparameters contains only the active parameters, but supported stuff is missing
        m_camera.withSupportedParameters(true);
    }



    /*public void setOptions(CameraSequence.Options paramOptions)
    {

            if (paramOptions == null) {
                paramOptions = new CameraSequence.Options();
            }
            paramOptions.setOption("AUTO_RELEASE_LOCK_ENABLED", true);
        cameraSequence.setReleaseLock(false);
    }*/

    private void dumpParameter() {
        StringTokenizer localStringTokenizer = new StringTokenizer(((Camera.Parameters)getParameters()).flatten(), ";");
        while (localStringTokenizer.hasMoreElements())
            Log.d(TAG, localStringTokenizer.nextToken());

        List<String> tmp = null;
        if (isImageStabSupported())
        {
            Log.d(TAG,"getSupportedImageStabModes");
            logList(getSupportedImageStabModes());
        }
        if (isLiveSlowShutterSupported()) {
            Log.d(TAG, "getSupportedLiveSlowShutterModes");
            logArray(getSupportedLiveSlowShutterModes());
        }
    }

    private void logList(List<String> list)
    {
        String st = new String();
        for (String s : list)
            st += s+",";
        Log.d(TAG,st);
    }
    private void logArray(String[] list)
    {
        String st = new String();
        for (String s : list)
            st += s+",";
        Log.d(TAG,st);
    }
}

