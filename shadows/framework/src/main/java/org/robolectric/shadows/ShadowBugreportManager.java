package org.robolectric.shadows;

import static android.os.Build.VERSION_CODES.Q;

import android.os.BugreportManager;
import android.os.BugreportManager.BugreportCallback;
import android.os.BugreportParams;
import android.os.ParcelFileDescriptor;
import java.io.IOException;
import java.util.concurrent.Executor;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * Implementation of {@link android.os.BugreportManager}.
 *
 * <p>This class is not available in the public Android SDK, but it is available for system apps.
 */
@Implements(value = BugreportManager.class, minSdk = Q, isInAndroidSdk = false)
public class ShadowBugreportManager {

  private boolean hasPermission = true;

  private ParcelFileDescriptor bugreportFd;
  private ParcelFileDescriptor screenshotFd;
  private Executor executor;
  private BugreportCallback callback;

  /**
   * Starts a bugreport with which can execute callback methods on the provided executor.
   *
   * <p>If bugreport already in progress, {@link BugreportCallback#onError} will be executed.
   */
  @Implementation
  protected void startBugreport(
      ParcelFileDescriptor bugreportFd,
      ParcelFileDescriptor screenshotFd,
      BugreportParams params,
      Executor executor,
      BugreportCallback callback) {
    enforcePermission("startBugreport");
    if (isBugreportInProgress()) {
      executor.execute(
          () -> callback.onError(BugreportCallback.BUGREPORT_ERROR_ANOTHER_REPORT_IN_PROGRESS));
    } else {
      this.bugreportFd = bugreportFd;
      this.screenshotFd = screenshotFd;
      this.executor = executor;
      this.callback = callback;
    }
  }

  /** Cancels bugreport in progress and executes {@link BugreportCallback#onError}. */
  @Implementation
  protected void cancelBugreport() {
    enforcePermission("cancelBugreport");
    executeOnError(BugreportCallback.BUGREPORT_ERROR_RUNTIME);
  }

  /** Executes {@link BugreportCallback#onProgress} on the provided Executor. */
  public void executeOnProgress(float progress) {
    if (isBugreportInProgress()) {
      BugreportCallback callback = this.callback;
      executor.execute(() -> callback.onProgress(progress));
    }
  }

  /** Executes {@link BugreportCallback#onError} on the provided Executor. */
  public void executeOnError(int errorCode) {
    if (isBugreportInProgress()) {
      BugreportCallback callback = this.callback;
      executor.execute(() -> callback.onError(errorCode));
    }
    resetParams();
  }

  /** Executes {@link BugreportCallback#onFinished} on the provided Executor. */
  public void executeOnFinished() {
    if (isBugreportInProgress()) {
      BugreportCallback callback = this.callback;
      executor.execute(callback::onFinished);
    }
    resetParams();
  }

  public boolean isBugreportInProgress() {
    return executor != null && callback != null;
  }

  /**
   * Simulates if the calling process has the required permissions to call BugreportManager methods.
   *
   * <p>If {@code hasPermission} is false, {@link #startBugreport} and {@link #cancelBugreport} will
   * throw {@link SecurityException}.
   */
  public void setHasPermission(boolean hasPermission) {
    this.hasPermission = hasPermission;
  }

  private void enforcePermission(String message) {
    if (!hasPermission) {
      throw new SecurityException(message);
    }
  }

  private void resetParams() {
    try {
      bugreportFd.close();
      if (screenshotFd != null) {
        screenshotFd.close();
      }
    } catch (IOException e) {
      // ignore.
    }
    bugreportFd = null;
    screenshotFd = null;
    executor = null;
    callback = null;
  }
}
