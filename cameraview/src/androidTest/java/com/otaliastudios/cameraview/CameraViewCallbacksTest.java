package com.otaliastudios.cameraview;


import android.content.Context;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import android.view.ViewGroup;

import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.engine.CameraEngine;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;
import com.otaliastudios.cameraview.gesture.Gesture;
import com.otaliastudios.cameraview.gesture.GestureAction;
import com.otaliastudios.cameraview.internal.utils.Task;
import com.otaliastudios.cameraview.engine.MockCameraEngine;
import com.otaliastudios.cameraview.preview.MockCameraPreview;
import com.otaliastudios.cameraview.preview.CameraPreview;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link CameraView#mCameraCallbacks} dispatch functions.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class CameraViewCallbacksTest extends BaseTest {

    private CameraView camera;
    private CameraListener listener;
    private FrameProcessor processor;
    private MockCameraEngine mockController;
    private MockCameraPreview mockPreview;
    private Task<Boolean> task;

    @Before
    public void setUp() {
        ui(new Runnable() {
            @Override
            public void run() {
                Context context = context();
                listener = mock(CameraListener.class);
                processor = mock(FrameProcessor.class);
                camera = new CameraView(context) {

                    @NonNull
                    @Override
                    protected CameraEngine instantiateCameraController(@NonNull CameraEngine.Callback callback) {
                        mockController = new MockCameraEngine(callback);
                        return mockController;
                    }

                    @NonNull
                    @Override
                    protected CameraPreview instantiatePreview(@NonNull Context context, @NonNull ViewGroup container) {
                        mockPreview = new MockCameraPreview(context, container);
                        return mockPreview;
                    }

                    @Override
                    protected boolean checkPermissions(@NonNull Audio audio) {
                        return true;
                    }
                };
                camera.instantiatePreview();
                camera.addCameraListener(listener);
                camera.addFrameProcessor(processor);
                task = new Task<>(true);
            }
        });
    }

    @After
    public void tearDown() {
        camera = null;
        mockController = null;
        mockPreview = null;
        listener = null;
    }

    // Completes our task.
    private Stubber completeTask() {
        return doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                task.end(true);
                return null;
            }
        });
    }

    @Test
    public void testDontDispatchIfRemoved() {
        camera.removeCameraListener(listener);
        completeTask().when(listener).onCameraOpened(null);
        camera.mCameraCallbacks.dispatchOnCameraOpened(null);

        assertNull(task.await(200));
        verify(listener, never()).onCameraOpened(null);
    }

    @Test
    public void testDontDispatchIfCleared() {
        camera.clearCameraListeners();
        completeTask().when(listener).onCameraOpened(null);
        camera.mCameraCallbacks.dispatchOnCameraOpened(null);

        assertNull(task.await(200));
        verify(listener, never()).onCameraOpened(null);
    }

    @Test
    public void testDispatchOnCameraOpened() {
        completeTask().when(listener).onCameraOpened(null);
        camera.mCameraCallbacks.dispatchOnCameraOpened(null);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onCameraOpened(null);
    }

    @Test
    public void testDispatchOnCameraClosed() {
        completeTask().when(listener).onCameraClosed();
        camera.mCameraCallbacks.dispatchOnCameraClosed();

        assertNotNull(task.await(200));
        verify(listener, times(1)).onCameraClosed();
    }

    @Test
    public void testDispatchOnVideoTaken() {
        VideoResult.Stub stub = new VideoResult.Stub();
        completeTask().when(listener).onVideoTaken(any(VideoResult.class));
        camera.mCameraCallbacks.dispatchOnVideoTaken(stub);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onVideoTaken(any(VideoResult.class));
    }

    @Test
    public void testDispatchOnPictureTaken() {
        PictureResult.Stub stub = new PictureResult.Stub();
        completeTask().when(listener).onPictureTaken(any(PictureResult.class));
        camera.mCameraCallbacks.dispatchOnPictureTaken(stub);
        assertNotNull(task.await(200));
        verify(listener, times(1)).onPictureTaken(any(PictureResult.class));
    }

    @Test
    public void testDispatchOnZoomChanged() {
        completeTask().when(listener).onZoomChanged(anyFloat(), any(float[].class), any(PointF[].class));
        camera.mCameraCallbacks.dispatchOnZoomChanged(0f, null);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onZoomChanged(anyFloat(), any(float[].class), any(PointF[].class));
    }

    @Test
    public void testDispatchOnExposureCorrectionChanged() {
        completeTask().when(listener).onExposureCorrectionChanged(0f, null, null);
        camera.mCameraCallbacks.dispatchOnExposureCorrectionChanged(0f, null, null);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onExposureCorrectionChanged(0f, null, null);
    }

    @Test
    public void testDispatchOnFocusStart() {
        // Enable tap gesture.
        // Can't mock package protected. camera.mTapGestureLayout = mock(TapGestureLayout.class);
        camera.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);

        PointF point = new PointF();
        completeTask().when(listener).onAutoFocusStart(point);
        camera.mCameraCallbacks.dispatchOnFocusStart(Gesture.TAP, point);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onAutoFocusStart(point);
        // Can't mock package protected. verify(camera.mTapGestureLayout, times(1)).onAutoFocusStart(point);
    }

    @Test
    public void testDispatchOnFocusEnd() {
        // Enable tap gesture.
        // Can't mock package protected. camera.mTapGestureLayout = mock(TapGestureLayout.class);
        camera.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS);

        PointF point = new PointF();
        boolean success = true;
        completeTask().when(listener).onAutoFocusEnd(success, point);
        camera.mCameraCallbacks.dispatchOnFocusEnd(Gesture.TAP, success, point);

        assertNotNull(task.await(200));
        verify(listener, times(1)).onAutoFocusEnd(success, point);
        // Can't mock package protected. verify(camera.mTapGestureLayout, times(1)).onAutoFocusEnd(success);
    }

    @Test
    public void testOrientationCallbacks() {
        completeTask().when(listener).onOrientationChanged(anyInt());
        camera.mCameraCallbacks.onDeviceOrientationChanged(90);
        assertNotNull(task.await(200));
        verify(listener, times(1)).onOrientationChanged(anyInt());
    }

    // TODO: test onShutter, here or elsewhere

    @Test
    public void testCameraError() {
        CameraException error = new CameraException(new RuntimeException("Error"));
        completeTask().when(listener).onCameraError(error);

        camera.mCameraCallbacks.dispatchError(error);
        assertNotNull(task.await(200));
        verify(listener, times(1)).onCameraError(error);
    }

    @Test
    public void testProcessFrame() {
        Frame mock = mock(Frame.class);
        completeTask().when(processor).process(mock);
        camera.mCameraCallbacks.dispatchFrame(mock);

        assertNotNull(task.await(200));
        verify(processor, times(1)).process(mock);
    }
}
