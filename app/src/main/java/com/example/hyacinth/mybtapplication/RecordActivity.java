package com.example.hyacinth.mybtapplication;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import    java.text.SimpleDateFormat;

public class RecordActivity extends Activity {
    private TextView text;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //private static String address = "98:D3:35:00:A4:A0";
    private static String address = "98:D3:35:00:A4:A0";
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket mmSocket;
    public String tt;
    private Socket mSocket;
    boolean BluetoothConnected;
    boolean ToDisconnected;
    ConnectThread cThread;
    Thread st;
    sendThread sThread;
    Thread sndt;
    BluetoothDevice device;
    BluetoothSocket tmp;
    private MyDatabaseHelper dbHelper;
    SQLiteDatabase db;
    boolean exit;
    Location globalLoc = null;

    private class sendThread implements Runnable {

        public void run() {
            //while (!exit) {
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
                finally {
                    try {
                        st.interrupt();
                    } catch (Exception x)
                    {
                        continue;
                    }
                }

            }
        }
    }

    private class ConnectThread implements Runnable {

        public void run() {
                try {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                    // MY_UUID is the app's UUID string, also used by the server code
                    mmSocket = tmp;
                    mBluetoothAdapter.cancelDiscovery();
                } catch (IOException e) {
                }
                // Cancel discovery because it will slow down the connection
//                exit = false;

                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception
                    mmSocket.connect();
                } catch (IOException connectException) {
                    // Unable to connect; close the socket and get out
                    try {
                        mmSocket.close();
                        BluetoothConnected = false;
                    } catch (IOException closeException) {
                    }
                }
            BluetoothConnected = true;
            //exit = false;
            while ((!exit) && (BluetoothConnected)){

                // Do work to manage the connection (in a separate thread)
                //tt = "Successful!";
                //text.setText("Successful!");
                try {
                    readFromConnectedSocket(mmSocket);
                } catch (InterruptedException e) {//send button pressed
                    //tt = "interrupted!!";
                    if (!BluetoothConnected)
                    {
                        //tt = "1";
                        return;
                    }
                    if ((exit) || (ToDisconnected)){
                        //tt = "2";
                        try {
                            mmSocket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        ToDisconnected = false;
                        BluetoothConnected = false;
                        return;
                    }
                    //tt = "Handling Exception!";
                    Cursor cursor = db.query("Data", null, null, null, null, null, null);
                    if (cursor.moveToFirst()) {
                        do {
                            int id = cursor.getInt(cursor.getColumnIndex("id"));
                            float pm = cursor.getFloat(cursor.getColumnIndex("value"));
                            float lat = cursor.getFloat(cursor.getColumnIndex("LAT"));
                            float lot = cursor.getFloat(cursor.getColumnIndex("LOT"));
                            long time = cursor.getLong(cursor.getColumnIndex("TIME"));
                            JSONObject obj = new JSONObject();
                            try {
                                obj.put("user", "abc");
                                obj.put("PM", pm);
                                obj.put("LAT", lat);
                                obj.put("LOT", lot);
                                obj.put("TIME", time);
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                            //tt = pm+"";
                            if ((int) pm != 233) {
                                mSocket.connect();
                                mSocket.emit("senddata", obj.toString());
                                //tt = "Message Sent!";
                                db.delete("Data", "id = ?", new String[]{String.valueOf(id)});
                            }

                        } while (cursor.moveToNext());
                    }
                    cursor.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                /*try {
                    mmSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }*/
            }
        }

        public void readFromConnectedSocket(BluetoothSocket mmSocket) throws InterruptedException, IOException {
            InputStream mmInStream;
            OutputStream mmOutStream;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            FileOutputStream out = null;
            BufferedWriter writer = null;
            byte[] buffer = new byte[1024];  // buffer store for the stream
            String tempt, tempFloat[];
            //ContentValues values = new ContentValues();
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
                @Override
                public void onLocationChanged(Location location)
                {
                    globalLoc = location;
                }
            };
            try {
                out = openFileOutput("dataWrite", Context.MODE_PRIVATE);
                writer = new BufferedWriter(new OutputStreamWriter(out));
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Get the input and output streams, using temp objects because
            // member streams are final
            int bytes = 0; // bytes returned from read()
            tmpIn = mmSocket.getInputStream();
            tmpOut = mmSocket.getOutputStream();
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            //tt = "Ready to read";
            //GPS
            String provider;
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, locationListener);

            List<String> providerList = locationManager.getProviders(true);
            if (providerList.contains(LocationManager.GPS_PROVIDER))
                provider = LocationManager.GPS_PROVIDER;
            else if (providerList.contains(LocationManager.NETWORK_PROVIDER))
                provider = LocationManager.NETWORK_PROVIDER;
            else return;
            /*if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
            }
            */
            Location location;// = locationManager.getLastKnownLocation(provider);
            //tt = "Location get!";

            Calendar calendar = Calendar.getInstance();
            //calendar.clear();
            //calendar.set(1970, 0, 1);
            TimeZone tz = TimeZone.getTimeZone("GMT");
            //TimeZone tz =TimeZone.getTimeZone("Asia/Shanghai"); TimeZone.setDefault(tz);
            calendar.setTimeZone(tz);
            // Keep listening to the InputStream until an exception occurs
            while (!exit) {
                Thread.sleep(1000);
                // Read from the InputStream
                for (int i = 0; i < 1024; i++) buffer[i] = 0;
                bytes = mmInStream.read(buffer);
                //tt = new String(buffer);
                //tempt = tt;
                tempt = new String(buffer);
                tempFloat = tempt.split("\\r?\\n", 0);
                for (int i = 0; i < tempFloat.length - 2; i++) {
                    //if (tempFloat[i].equals("233")) continue;
                    ContentValues values = new ContentValues();
                    values.clear();
                    values.put("value", String.valueOf(tempFloat[i]));
                    location = null;
                    //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,4000, 0,locationListener);
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    while (location == null)
                    {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                        //tt = "looping...";
                        /*provider = LocationManager.NETWORK_PROVIDER;
                        location = locationManager.getLastKnownLocation(provider);*/
                        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000, 0,locationListener);

                    }
                    //location = globalLoc;
                    if (location != null)
                    {
                        //values.put("LAT", 0);
                        //values.put("LOT", 0);
                        values.put("LAT", String.valueOf(location.getLatitude()));
                        values.put("LOT", String.valueOf(location.getLongitude()));
                        long t = calendar.getTimeInMillis();
                        values.put("TIME", calendar.getTimeInMillis());
                        db.insert("Data", null, values);
                        //tt = "db inserted!";
                    }
                }
                //System.out.println(tempFloat);
                // Send the obtained bytes to the UI activity
                //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                //.sendToTarget();
            }
            //tt = "Bluetooth Disconnected!";
        }
    }
    //在类里声明一个Handler
    Handler mTimeHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 0) {
                text.setText(tt);
                sendEmptyMessageDelayed(0, 500);
            }
        }
    };

    private Button recvButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        exit = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        dbHelper = new MyDatabaseHelper(this, "DataStore.db", null, 1);
        db = dbHelper.getWritableDatabase();
        db.delete("Data",null,null);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        text = (TextView) findViewById(R.id.text);
        tt="Hello World!";
        //在你的onCreate的类似的方法里面启动这个Handler就可以了：
        mTimeHandler.sendEmptyMessageDelayed(0, 500);
        Set<BluetoothDevice> bonddevices = mBluetoothAdapter.getBondedDevices();
        if (bonddevices.size()>0) {
            for(Iterator<BluetoothDevice> iterator = bonddevices.iterator(); iterator.hasNext();){
                BluetoothDevice bluetoothDevice=(BluetoothDevice)iterator.next();
                if (bluetoothDevice.getName().equals("SPP-CA")) {
                    //device = mBluetoothAdapter.getRemoteDevice(address);
                    device = mBluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress());
                    tt = "device get!";
                    break;
                }
                //System.out.println("设备："+bluetoothDevice.getName() + " " + bluetoothDevice.getAddress());
            }
        }
        //device = mBluetoothAdapter.getRemoteDevice(address);
        tmp = null;
        BluetoothConnected = false;

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            tt = "Device does not support Bluetooth";
            //text.setText("Device does not support Bluetooth");
        }
        sThread = new sendThread();
        sndt = new Thread(sThread);
        //cHandler=new Handler();
        //cHandler.post(cThread);
        ToDisconnected = false;
        cThread = new ConnectThread();

        recvButton = (Button)findViewById(R.id.recvButton);
        recvButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tt = "正在接收...";
                if (!BluetoothConnected)
                {
                    //ToDisconnected = false;
                    st = new Thread(cThread);
                    st.start();
                }
            }
        });

        stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tt = "已停止接受";
                if (BluetoothConnected)
                {
                    ToDisconnected = true;
                    try {
                        st.interrupt();
                    } catch (Exception x)
                    {
                        //tt = "wrong!";
                    }
                    //ToDisconnected = false;
                }
            }
        });

        Button mButton = (Button) findViewById(R.id.mapButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecordActivity.this,MapFullscreenActivity.class);
                startActivity(intent);
            }
        });
        //get Socket
        try {
            mSocket = IO.socket("http://133.130.116.215:3000/");
            mSocket.on("complete", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                }
            });
            mSocket.connect();
        } catch (URISyntaxException x) {
            x.printStackTrace();
        }
        tt = "Connected to Server";
        sndt.start();
    }
    protected void onDestroy(){
        exit = true;
        try {
            st.interrupt();
        }
        catch (Exception x){}
        try {
            sndt.interrupt();
        }
        catch (Exception x){}
        mSocket.close();
        try {
            mmSocket.close();
            BluetoothConnected = false;
        }
        catch (Exception x){}
        //cThread.removeCallbacks(mRunnable);
        super.onDestroy();
    }
    public class MyDatabaseHelper extends SQLiteOpenHelper {
        public static final String CREATE_DATA = "create table Data ("
                +"id integer primary key autoincrement, "
                +"LAT real, "
                +"LOT real, "
                +"TIME INTEGER, "
                +"value real)";
        private Context mContext;

        public MyDatabaseHelper(Context contex, String name, SQLiteDatabase.CursorFactory factory, int version)
        {
            super(contex, name, factory, version);
            mContext = contex;
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(CREATE_DATA);
        }
        public void onUpgrade(SQLiteDatabase db, int OldVersion, int NewVersion)
        {

        }
    }


}
