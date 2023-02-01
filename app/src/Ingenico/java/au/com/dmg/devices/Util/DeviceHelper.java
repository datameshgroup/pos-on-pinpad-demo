package au.com.dmg.devices.Util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.usdk.apiservice.aidl.BaseError;
import com.usdk.apiservice.aidl.DeviceServiceData;
import com.usdk.apiservice.aidl.UDeviceService;
import com.usdk.apiservice.aidl.device.UDeviceManager;
import com.usdk.apiservice.aidl.printer.UPrinter;
import com.usdk.apiservice.aidl.scanner.UScanner;
import com.usdk.apiservice.limited.DeviceServiceLimited;


/**
 * The class of device service auxiliary,
 * implements the connection with the equipment service
 * and provides the interface for accessing each device.
 *
 */
public final class DeviceHelper implements ServiceConnection {
	private static final String TAG = "DeviceHelper";
	private static final int MAX_RETRY_COUNT = 3;
	private static final long RETRY_INTERVALS = 3000;

	private static DeviceHelper me = new DeviceHelper();

	private Context context;
	private ServiceReadyListener serviceListener;

	private int retry = 0;
	private volatile boolean isBinded = false;
	private UDeviceService deviceService;


	public String getErrorDetail(int error) {
		String message = getErrorMessage(error);
		if (error < 0) {
			return message + "[" + error + "]";
		}
		return message + String.format("[0x%02X]", error);
	}

	public String getErrorMessage(int error) {
		String message;
		switch (error) {
			case BaseError.SERVICE_CRASH: message = "SERVICE_CRASH"; break;
			case BaseError.REQUEST_EXCEPTION: message = "REQUEST_EXCEPTION"; break;
			case BaseError.ERROR_CANNOT_EXECUTABLE: message = "ERROR_CANNOT_EXECUTABLE"; break;
			case BaseError.ERROR_INTERRUPTED: message = "ERROR_INTERRUPTED"; break;
			case BaseError.ERROR_HANDLE_INVALID: message = "ERROR_HANDLE_INVALID"; break;
			default:
				message = "Unknown error";
		}
		return message;
	}

	public static DeviceHelper me() {
		return me;
	}

	public void init(Context context) {
		this.context = context;
	}

	public void bindService() {
		if (isBinded) {
			return;
		}

		Intent service = new Intent("com.usdk.apiservice");
		service.setPackage("com.usdk.apiservice");
		boolean bindSucc = context.bindService(service, me, Context.BIND_AUTO_CREATE);

		if (!bindSucc && retry++ < MAX_RETRY_COUNT) {
			Log.e(TAG, "=> bind fail, rebind (" + retry +")");
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					bindService();
				}
			}, RETRY_INTERVALS);
		}
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.d(TAG, "=> onServiceConnected");

		retry = 0;
		isBinded = true;

		deviceService = UDeviceService.Stub.asInterface(service);

		DeviceServiceLimited.bind(context, deviceService, new DeviceServiceLimited.ServiceBindListener() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "=> DeviceServiceLimited | bindSuccess");
			}

			@Override
			public void onFail() {
				Log.e(TAG, "=> bind DeviceServiceLimited fail");
			}
		});

		notifyReady();
	}

	private void notifyReady() {
		if (serviceListener != null) {
			try {
				serviceListener.onReady(deviceService.getVersion());
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		Log.e(TAG, "=> onServiceDisconnected");

		deviceService = null;
		isBinded = false;
		DeviceServiceLimited.unbind(context);
		bindService();
	}

	public void unbindService() {
		if (isBinded) {
			Log.e(TAG, "=> unbindService");
			context.unbindService(this);
			DeviceServiceLimited.unbind(context);
			isBinded = false;
		}
	}

	public void register(boolean useEpayModule) throws IllegalStateException {
		try {
			Bundle param = new Bundle();
			param.putBoolean(DeviceServiceData.USE_EPAY_MODULE, useEpayModule);
			deviceService.register(param, new Binder());
		} catch (RemoteException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void unregister() throws IllegalStateException {
		try {
			deviceService.unregister(null);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw new IllegalStateException(e.getMessage());
		}
	}

	public void debugLog(boolean open) {
		try {
			Bundle logOption = new Bundle();
			logOption.putBoolean(DeviceServiceData.COMMON_LOG, open);
			deviceService.debugLog(logOption);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public UDeviceManager getDeviceManager() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getDeviceManager();
			}
		}.start();
		return UDeviceManager.Stub.asInterface(iBinder);
	}

	public UPrinter getPrinter() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getPrinter();
			}
		}.start();
		return UPrinter.Stub.asInterface(iBinder);
	}

	public UScanner getScanner(final int cameraId) throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getScanner(cameraId);
			}
		}.start();
		return UScanner.Stub.asInterface(iBinder);
	}


    abstract class IBinderCreator {
		IBinder start() throws IllegalStateException {
			if (deviceService == null) {
				bindService();
				throw new IllegalStateException("Service unbound,please retry later!");
			}
			try {
				return create();

			} catch (DeadObjectException e) {
				deviceService = null;
				throw new IllegalStateException("Service process has stopped,please retry latter!");

			} catch (RemoteException | SecurityException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		abstract IBinder create() throws RemoteException;
	}

	public interface ServiceReadyListener {
		void onReady(String version);
	}

}
