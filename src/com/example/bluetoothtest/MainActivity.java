package com.example.bluetoothtest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.security.auth.PrivateCredentialPermission;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {
	BluetoothManager bManager;
	// ListView lvDevices;
	BluetoothDevice device;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_main);

		bManager = new BluetoothManager(this);

		// if (bManager.adapter == null) {
		if (bManager.getBluetoothAdapter() == null) {
			Toast.makeText(this, "no bluebooth on this device", Toast.LENGTH_SHORT).show();
			finish();
		}
		Log.d(BLUETOOTH_SERVICE, "创建一个 BluetoothManageer");

		// lvDevices = (ListView) findViewById(android.R.id.list);
		// lvDevices.setAdapter(bManager.arrayAdapter);
		// lvDevices.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		// lvDevices.setOnItemClickListener(clickListener);

		setListAdapter(bManager.getspAdapter());
	}

	@Override
	protected void onResume() {
		super.onResume();

		Log.d(BLUETOOTH_SERVICE, "使能蓝牙");

		if (bManager.enableBluetooth() == true) {
			BluetoothManager.AcceptThread acceptThread = bManager.new AcceptThread() {
				public void manageSocket(BluetoothSocket socket) {
					BluetoothManager.ConnectedThread cThread = bManager.new ConnectedThread(socket);
					cThread.start();
				}
			};
			acceptThread.start();
		}

		bManager.searchDevices();

	}

	@Override
	protected void onPause() {
		super.onPause();
		bManager.cancel();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if ((requestCode == bManager.REQUEST_ENABLE_BT) && (resultCode == RESULT_OK)) {
			Log.d(BLUETOOTH_SERVICE, "返回使能蓝牙成功");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String address = (String) bManager.getList().get(position).get("Mac");
		BluetoothDevice device = bManager.getBluetoothAdapter().getRemoteDevice(address);
		Log.d(BLUETOOTH_SERVICE, "Name:" + bManager.getList().get(position).get("Name"));
		Log.d(BLUETOOTH_SERVICE, "Mac:" + address);

		BluetoothManager.ConnectThread cThread = bManager.new ConnectThread(device) {
			@Override
			public void manageSocket(BluetoothSocket socket) {
				String msgString = "Hello, Eason";
				try {
					byte[] buffer = msgString.getBytes("UTF-8");
					InputStream is = socket.getInputStream();
					OutputStream os = socket.getOutputStream();
					while (true) {
						Log.d(BLUETOOTH_SERVICE, "处理Client Socket");
						try {
							sleep(1000);
							os.write(buffer);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}

		};
		cThread.start();

		super.onListItemClick(l, v, position, id);
	}
}
