package au.com.dmg.terminalposdemo.Util;

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
import com.usdk.apiservice.aidl.ModuleName;
import com.usdk.apiservice.aidl.UDeviceService;
import com.usdk.apiservice.aidl.algorithm.UAlgorithm;
import com.usdk.apiservice.aidl.beeper.UBeeper;
import com.usdk.apiservice.aidl.cashbox.UCashBox;
import com.usdk.apiservice.aidl.decodeengine.UDecodeEngine;
import com.usdk.apiservice.aidl.device.UDeviceManager;
import com.usdk.apiservice.aidl.deviceadmin.UDeviceAdmin;
import com.usdk.apiservice.aidl.digled.UDigled;
import com.usdk.apiservice.aidl.dock.DockName;
import com.usdk.apiservice.aidl.dock.UBTDock;
import com.usdk.apiservice.aidl.dock.UWifiDock;
import com.usdk.apiservice.aidl.dock.ethernet.UDockEthernet;
import com.usdk.apiservice.aidl.dock.serialport.UDockPort;
import com.usdk.apiservice.aidl.emv.UEMV;
import com.usdk.apiservice.aidl.ethernet.UEthernet;
import com.usdk.apiservice.aidl.exscanner.UExScanner;
import com.usdk.apiservice.aidl.felicareader.UFelicaReader;
import com.usdk.apiservice.aidl.fiscal.UFiscal;
import com.usdk.apiservice.aidl.icreader.DriverID;
import com.usdk.apiservice.aidl.icreader.UAT1604Reader;
import com.usdk.apiservice.aidl.icreader.UAT1608Reader;
import com.usdk.apiservice.aidl.icreader.UAT24CxxReader;
import com.usdk.apiservice.aidl.icreader.UICCpuReader;
import com.usdk.apiservice.aidl.icreader.UPSamReader;
import com.usdk.apiservice.aidl.icreader.USIM4428Reader;
import com.usdk.apiservice.aidl.icreader.USIM4442Reader;
import com.usdk.apiservice.aidl.innerscanner.UInnerScanner;
import com.usdk.apiservice.aidl.led.ULed;
import com.usdk.apiservice.aidl.lki.ULKITool;
import com.usdk.apiservice.aidl.magreader.UMagReader;
import com.usdk.apiservice.aidl.magreader.industry.UIndustryMagReader;
import com.usdk.apiservice.aidl.mifare.MifareManagerType;
import com.usdk.apiservice.aidl.mifare.UDesFireManager;
import com.usdk.apiservice.aidl.mifare.UMifareKeyManager;
import com.usdk.apiservice.aidl.networkmanager.UNetWorkManager;
import com.usdk.apiservice.aidl.onguard.UOnGuard;
import com.usdk.apiservice.aidl.paramfile.UParamFile;
import com.usdk.apiservice.aidl.pinpad.DeviceName;
import com.usdk.apiservice.aidl.pinpad.KAPId;
import com.usdk.apiservice.aidl.pinpad.UPinpad;
import com.usdk.apiservice.aidl.printer.UPrinter;
import com.usdk.apiservice.aidl.resetfactory.UResetFactory;
import com.usdk.apiservice.aidl.rfreader.URFReader;
import com.usdk.apiservice.aidl.scanner.UScanner;
import com.usdk.apiservice.aidl.serialport.USerialPort;
import com.usdk.apiservice.aidl.signpanel.USignPanel;
import com.usdk.apiservice.aidl.system.USystem;
import com.usdk.apiservice.aidl.system.application.UApplication;
import com.usdk.apiservice.aidl.system.input.UInputManager;
import com.usdk.apiservice.aidl.system.keyboard.UKeyboard;
import com.usdk.apiservice.aidl.system.location.ULocation;
import com.usdk.apiservice.aidl.system.nfc.UNfc;
import com.usdk.apiservice.aidl.system.process.UProcess;
import com.usdk.apiservice.aidl.system.security.UKeyChain;
import com.usdk.apiservice.aidl.system.setting.USetting;
import com.usdk.apiservice.aidl.system.statusbar.UStatusBar;
import com.usdk.apiservice.aidl.system.storage.UStorage;
import com.usdk.apiservice.aidl.system.systemproperty.USystemProperty;
import com.usdk.apiservice.aidl.system.telephony.UTelephony;
import com.usdk.apiservice.aidl.system.usb.UUsb;
import com.usdk.apiservice.aidl.systemstatistics.USystemStatistics;
import com.usdk.apiservice.aidl.tms.UTMS;
import com.usdk.apiservice.aidl.update.UUpdate;
import com.usdk.apiservice.aidl.usbdevice.UUsbDevice;
import com.usdk.apiservice.aidl.vectorprinter.UVectorPrinter;
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

	public void setServiceListener(ServiceReadyListener listener) {
		serviceListener = listener;
		if (isBinded) {
			notifyReady();
		}
	}

	public void bindService() {
		if (isBinded) {
			return;
		}

		Intent service = new Intent("com.usdk.apiservice");
		service.setPackage("com.usdk.apiservice");
		boolean bindSucc = context.bindService(service, me, Context.BIND_AUTO_CREATE);

		// 绑定失败, 则重新绑定
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

	public UAlgorithm getAlgorithm() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getAlgorithm();
			}
		}.start();
		return UAlgorithm.Stub.asInterface(iBinder);
	}

	public UBeeper getBeeper() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getBeeper();
			}
		}.start();
		return UBeeper.Stub.asInterface(iBinder);
	}

	public UCashBox getCashBox() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getCashBox();
			}
		}.start();
		return UCashBox.Stub.asInterface(iBinder);
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

	public UDigled getDigled() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getDigled();
			}
		}.start();
		return UDigled.Stub.asInterface(iBinder);
	}

	public UEMV getEMV() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getEMV();
			}
		}.start();
		return UEMV.Stub.asInterface(iBinder);
	}

	public UEthernet getEthernet() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getEthernet();
			}
		}.start();
		return UEthernet.Stub.asInterface(iBinder);
	}

	public UExScanner getExScanner(final int scannerType, final String deviceName) throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle param = new Bundle();
				param.putString(DeviceServiceData.DEVICE_NAME, deviceName);
				return deviceService.getExScanner(scannerType, param);
			}
		}.start();
		return UExScanner.Stub.asInterface(iBinder);
	}

	public UICCpuReader getICCpuReader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getICReader(DriverID.ICCPU, null);
			}
		}.start();
		return UICCpuReader.Stub.asInterface(iBinder);
	}

	public UAT24CxxReader getAT24CXXReader(final int cardType) throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle param = new Bundle();
				param.putInt("cardType", cardType);
				return deviceService.getICReader(DriverID.AT24CXX, param);
			}
		}.start();
		return UAT24CxxReader.Stub.asInterface(iBinder);
	}

	public UAT1604Reader getAT1604Reader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getICReader(DriverID.AT1604, null);
			}
		}.start();
		return UAT1604Reader.Stub.asInterface(iBinder);
	}

	public UAT1608Reader getAT1608Reader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getICReader(DriverID.AT1608, null);
			}
		}.start();
		return UAT1608Reader.Stub.asInterface(iBinder);
	}

	public USIM4428Reader getSIM4428Reader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getICReader(DriverID.SIM4428, null);
			}
		}.start();
		return USIM4428Reader.Stub.asInterface(iBinder);
	}

	public USIM4442Reader getSIM4442Reader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getICReader(DriverID.SIM4442, null);
			}
		}.start();
		return USIM4442Reader.Stub.asInterface(iBinder);
	}

	public ULed getLed(final String dviceName) throws IllegalStateException {
		IBinder iBinder = new IBinderCreator() {
			@Override
			IBinder create() throws RemoteException {
				Bundle param = new Bundle();
				param.putString(DeviceServiceData.RF_DEVICE_NAME, dviceName);
				return deviceService.getLed(param);
			}
		}.start();
		return ULed.Stub.asInterface(iBinder);
	}

	public ULKITool getLKITool() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getLKITool();
			}
		}.start();
		return ULKITool.Stub.asInterface(iBinder);
	}

	public UMagReader getMagReader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getMagReader();
			}
		}.start();
		return UMagReader.Stub.asInterface(iBinder);
	}

	public UPinpad getPinpad(final KAPId kapId, final int keySystem, final String deviceName) throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getPinpad(kapId, keySystem, deviceName);
			}
		}.start();
		return UPinpad.Stub.asInterface(iBinder);
	}

	public URFReader getRFReader(final String deviceName) throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle param = new Bundle();
				param.putString("rfDeviceName", deviceName);
				return deviceService.getRFReader(param);
			}
		}.start();
		return URFReader.Stub.asInterface(iBinder);
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

	public USerialPort getSerialPort(final String deviceName) throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getSerialPort(deviceName);
			}
		}.start();
		return USerialPort.Stub.asInterface(iBinder);
	}

	public UUsbDevice getUsbDevice(final String usbType) throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle param = new Bundle();
				param.putString(DeviceServiceData.USB_TYPE, usbType);
				return deviceService.getModule(ModuleName.VIRTUAL_USB_DEVICE, param);
			}
		}.start();
		return UUsbDevice.Stub.asInterface(iBinder);
	}

	public UTMS getTMS() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getTMS();
			}
		}.start();
		return UTMS.Stub.asInterface(iBinder);
	}

	public UPSamReader getPSamReader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle bundle = new Bundle();
				bundle.putInt(DeviceServiceData.SLOT, 1);
				return deviceService.getICReader(DriverID.PSAMCARD, bundle);
			}
		}.start();
		return UPSamReader.Stub.asInterface(iBinder);
	}

	public UParamFile getParamFile(final String moduleName, final String fileName) throws IllegalStateException {
		IBinder binder = new IBinderCreator() {
			@Override
			IBinder create() throws RemoteException, SecurityException {
				return deviceService.getParamFile(moduleName, fileName);
			}
		}.start();
		return UParamFile.Stub.asInterface(binder);
	}

	public UInnerScanner getInnerScanner() throws IllegalStateException {
		IBinder binder = new IBinderCreator() {
			@Override
			IBinder create() throws RemoteException, SecurityException {
				return deviceService.getInnerScanner();
			}
		}.start();
		return UInnerScanner.Stub.asInterface(binder);
	}

	public USystemStatistics getSystemStatistics() throws IllegalStateException {
		IBinder binder = new IBinderCreator() {
			@Override
			IBinder create() throws RemoteException, SecurityException {
				return deviceService.getSystemStatistics();
			}
		}.start();
		return USystemStatistics.Stub.asInterface(binder);
	}

	public UIndustryMagReader getIndustryMagReader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getIndustryMagReader();
			}
		}.start();
		return UIndustryMagReader.Stub.asInterface(iBinder);
	}

	public USignPanel getSignPanel(final String deviceName) throws IllegalStateException {
		IBinder binder = new IBinderCreator() {
			@Override
			IBinder create() throws RemoteException, SecurityException {
				Bundle param = new Bundle();
				param.putString(DeviceServiceData.DEVICE_NAME, deviceName);
				return deviceService.getSignPanel(param);
			}
		}.start();
		return USignPanel.Stub.asInterface(binder);
	}

	public UFelicaReader getFelicaReader() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle param = new Bundle();
				return deviceService.getFelicaReader(param);
			}
		}.start();
		return UFelicaReader.Stub.asInterface(iBinder);
	}

	public UFiscal getFiscal() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle param = new Bundle();
				return deviceService.getFiscal(param);
			}
		}.start();
		return UFiscal.Stub.asInterface(iBinder);
	}

	public USystem getSystem() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getSystem();
			}
		}.start();
		return USystem.Stub.asInterface(iBinder);
	}

	public UUsb getUsb() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getUsb();
			}
		}.start();
		return UUsb.Stub.asInterface(iBinder);
	}

	public USetting getSetting() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getSetting();
			}
		}.start();
		return USetting.Stub.asInterface(iBinder);
	}

	public UProcess getProcess() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getProcess();
			}
		}.start();
		return UProcess.Stub.asInterface(iBinder);
	}

	public UStorage getStorage() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getStorage();
			}
		}.start();
		return UStorage.Stub.asInterface(iBinder);
	}

	public UKeyChain getKeyChain() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getKeyChain();
			}
		}.start();
		return UKeyChain.Stub.asInterface(iBinder);
	}

	public UTelephony getTelephony() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getTelephony();
			}
		}.start();
		return UTelephony.Stub.asInterface(iBinder);
	}

	public UApplication getApplication() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getApplication();
			}
		}.start();
		return UApplication.Stub.asInterface(iBinder);
	}

	public USystemProperty getSystemProperty() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getSystemProperty();
			}
		}.start();
		return USystemProperty.Stub.asInterface(iBinder);
	}

	public UInputManager getInputManager() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getInputManager();
			}
		}.start();
		return UInputManager.Stub.asInterface(iBinder);
	}

	public UKeyboard getKeyboard() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getKeyboard();
			}
		}.start();
		return UKeyboard.Stub.asInterface(iBinder);
	}

	public ULocation getLocation() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getLocation();
			}
		}.start();
		return ULocation.Stub.asInterface(iBinder);
	}

	public UNfc getNfc() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getNfc();
			}
		}.start();
		return UNfc.Stub.asInterface(iBinder);
	}

	public UStatusBar getStatusBar() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getSystem().getStatusBar();
			}
		}.start();
		return UStatusBar.Stub.asInterface(iBinder);
	}

	public UWifiDock getWifiDock() {
		return UWifiDock.Stub.asInterface(getDock(DockName.WIFI_DOCK));
	}

	public UBTDock getBTDock() {
		return UBTDock.Stub.asInterface(getDock(DockName.BT_DOCK));
	}

	private IBinder getDock(final String dockName) {
		return new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle param = new Bundle();
				param.putString(DeviceServiceData.DOCK_NAME, dockName);
				return deviceService.getDock(param);
			}
		}.start();
	}

	public UDockEthernet getWifiDockEthernet(final String portName) {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return getWifiDock().getDockEthernet(portName);
			}
		}.start();
		return UDockEthernet.Stub.asInterface(iBinder);
	}

	public UDockPort getWifiDockPort(final String portName) {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
                return getWifiDock().getDockPort(portName);
			}
		}.start();
		return UDockPort.Stub.asInterface(iBinder);
	}

	public UDockPort getBTDockPort(final String portName) {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
                return getBTDock().getDockPort(portName);
			}
		}.start();
		return UDockPort.Stub.asInterface(iBinder);
	}

	public UDesFireManager getDesFireManager(final String deviceName) {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle bundle = new Bundle();
				bundle.putInt(DeviceServiceData.MIFARE_MANAGER_TYPE, MifareManagerType.DESFIRE_MANAGER);
				bundle.putString(DeviceServiceData.RF_DEVICE_NAME, deviceName);
				return deviceService.getMifareManager(bundle);
			}
		}.start();
		return UDesFireManager.Stub.asInterface(iBinder);
	}

	public UMifareKeyManager getMifareKeyManager() {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				Bundle bundle = new Bundle();
				bundle.putInt(DeviceServiceData.MIFARE_MANAGER_TYPE, MifareManagerType.KEY_MANAGER);
				return deviceService.getMifareManager(bundle);
			}
		}.start();
		return UMifareKeyManager.Stub.asInterface(iBinder);
	}

	public UResetFactory getResetFactory() {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getResetFactory();
			}
		}.start();
		return UResetFactory.Stub.asInterface(iBinder);
	}

	public UVectorPrinter getVectorPrinter() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getVectorPrinter();
			}
		}.start();
		return UVectorPrinter.Stub.asInterface(iBinder);
	}

	public UDecodeEngine getDecodeEngine() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getDecodeEngine();
			}
		}.start();
		return UDecodeEngine.Stub.asInterface(iBinder);
	}

	public UOnGuard getOnGuard() {
		IBinder iBinder = new IBinderCreator() {
			@Override
			IBinder create() throws RemoteException {
				Bundle bundle = new Bundle();
				bundle.putString(DeviceServiceData.DEVICE_NAME, DeviceName.IPP);
				return deviceService.getOnGuard(bundle);
			}
		}.start();
		return UOnGuard.Stub.asInterface(iBinder);
	}

	public UUpdate getUpdate() {
		IBinder iBinder = new IBinderCreator() {
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getModule(ModuleName.UPDATE, null);
			}
		}.start();
		return UUpdate.Stub.asInterface(iBinder);
	}

	public UNetWorkManager getNetWorkManager() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getModule(ModuleName.NETWORK_MANAGER, new Bundle());
			}
		}.start();
		return UNetWorkManager.Stub.asInterface(iBinder);
	}

	public UDeviceAdmin getDeviceAdmin() throws IllegalStateException {
		IBinder iBinder = new IBinderCreator(){
			@Override
			IBinder create() throws RemoteException {
				return deviceService.getModule(ModuleName.DEVICE_ADMIN, new Bundle());
			}
		}.start();
		return UDeviceAdmin.Stub.asInterface(iBinder);
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

	/**
	 * 服务就绪监听器
	 */
	public interface ServiceReadyListener {
		void onReady(String version);
	}

}
