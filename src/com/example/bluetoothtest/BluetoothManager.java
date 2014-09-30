package com.example.bluetoothtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.SimpleAdapter;
import android.widget.Toast;

//http://www.cnblogs.com/greatverve/archive/2012/04/24/android-bluetooth.html
public class BluetoothManager {
	public final int REQUEST_ENABLE_BT = 1;

	private final Context context;
	private final static UUID MY_UUID = UUID
			.fromString(BluetoothUuidList.SerialPortServiceClass_UUID);

	private BluetoothAdapter bluetoothAdapter;
	// public List<String> mlist;
	// public ArrayAdapter<String> arrayAdapter;

	private List<Map<String, Object>> mList;
	private SimpleAdapter spAdapter;

	private BroadcastReceiver mybBroadcastReceiver;
	// 是否注册了 Receiver filter
	private boolean receiveRegistFlag = false;

	public BluetoothManager(Context context) {
		this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		this.context = context;
		// this.mlist = new ArrayList<String>();
		// this.arrayAdapter = new ArrayAdapter<String>(context,
		// android.R.layout.simple_list_item_single_choice,
		// this.mlist);

		mList = new ArrayList<Map<String, Object>>();
		spAdapter = new SimpleAdapter(context, mList, R.layout.deviceslist, new String[] { "Name",
				"Mac" }, new int[] { R.id.tvName, R.id.tvMac });
	}

	public BluetoothAdapter getBluetoothAdapter() {
		return bluetoothAdapter;
	}

	public SimpleAdapter getspAdapter() {
		return spAdapter;
	}

	public List<Map<String, Object>> getList() {
		return mList;
	}

	public boolean enableBluetooth() {
		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			((Activity) context).startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

			if (!bluetoothAdapter.isEnabled()) {
				Toast.makeText(context, "打开蓝牙失败", Toast.LENGTH_SHORT).show();
				return false;
			}

			Toast.makeText(context, "打开蓝牙成功", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(context, "蓝牙已经打开", Toast.LENGTH_SHORT).show();
		}

		bluetoothAdapter.enable();

		return true;
	}

	/**
	 * 
	 */
	public void ensureDiscoverable() {
		if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			context.startActivity(discoverableIntent);
		}
	}

	public void searchDevices() {
		mybBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (BluetoothDevice.ACTION_FOUND.equals(action)) {
					BluetoothDevice device = intent
							.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					// mlist.add("name:" + device.getName() + "\nmac:" +
					// device.getAddress());
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("Name", (device.getName() == null ? "null" : device.getName()));
					map.put("Mac", device.getAddress());

					if (mList.contains(map) == false) {
						mList.add(map);
					}

					Toast.makeText(context, "找到设备:" + device.getName(), Toast.LENGTH_SHORT).show();
				} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
					Toast.makeText(context, "搜索设备结束", Toast.LENGTH_SHORT).show();
				}

				// arrayAdapter.notifyDataSetChanged();
				spAdapter.notifyDataSetChanged();
			}
		};

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		context.registerReceiver(mybBroadcastReceiver, filter);
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		context.registerReceiver(mybBroadcastReceiver, filter);

		receiveRegistFlag = true;

		bluetoothAdapter.startDiscovery();
	}

	public void cancel() {
		if (receiveRegistFlag) {
			context.unregisterReceiver(mybBroadcastReceiver);
		}
	}

	public abstract class AcceptThread extends Thread {
		private BluetoothServerSocket mServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tempSocket = null;

			try {
				tempSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("eason", MY_UUID);
			} catch (IOException e) {
				e.printStackTrace();
			}

			mServerSocket = tempSocket;
		}

		public void run() {
			BluetoothSocket mSocket = null;

			while (true) {
				try {
					mSocket = mServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}

				if (mSocket != null) {
					// try {
					// mServerSocket.close();
					// } catch (IOException e) {
					// e.printStackTrace();
					// }

					manageSocket(mSocket);
					// ConnectedThread cThread = new ConnectedThread(mSocket);
					// cThread.start();
					// break;
				}
			}
		}

		public abstract void manageSocket(BluetoothSocket socket);

		public void cancel() {
			try {
				mServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public abstract class ConnectThread extends Thread {
		private BluetoothSocket clientSocket;
		private BluetoothDevice mDevice;

		public ConnectThread(BluetoothDevice device) {
			this.mDevice = device;

			try {
				clientSocket = this.mDevice.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				e.printStackTrace();
				Log.d(null, "获取 socket失败");
			}

		}

		public void run() {
			if (bluetoothAdapter.isDiscovering()) {
				bluetoothAdapter.cancelDiscovery();
			}

			try {
				clientSocket.connect();
				Log.d(null, "连接设备成功");
				manageSocket(clientSocket);
			} catch (IOException e) {
				Log.d(null, "trying fallback...");
			}
		}

		public abstract void manageSocket(BluetoothSocket socket);

		public void cancel() {
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public class ConnectedThread extends Thread {
		private BluetoothSocket socket;

		private InputStream inputStream;
		private OutputStream outputStream;

		public ConnectedThread(BluetoothSocket socket) {
			this.socket = socket;

			try {
				inputStream = socket.getInputStream();
				outputStream = socket.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {
			byte[] buf = new byte[1024];
			int len;

			while (true) {
				try {
					len = inputStream.read(buf);
					String revString = new String(buf, 0, len, "UTF-8");
					Log.d(null, revString);
				} catch (IOException e) {
					e.printStackTrace();
					// connection lost, need to do something
					break;
				}
			}
		}

		// public abstract void run();

		// public void write(byte[] buf) {
		// try {
		// outputStream.write(buf);
		// // TODO send data to ...
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }

		// public abstract void write(byte[] buf);

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
