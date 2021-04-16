package com.smart_seat.seat;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;

import com.smart_seat.seat.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Control extends AppCompatActivity {

   // Button btnOn, btnOff, btnDis;
    Button On, Off, Discnt, hrt, usg;
    TextView usgDisp,hrtDisp;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_control);

        //call the widgets
        On = (Button)findViewById(R.id.on_btn);
        Off = (Button)findViewById(R.id.off_btn);
        Discnt = (Button)findViewById(R.id.dis_btn);
        hrt = findViewById(R.id.hrt_btn);
        usg = findViewById(R.id.usg_btn);
        usgDisp = findViewById(R.id.usg_disp);
        hrtDisp = findViewById(R.id.hrtdisp);


        new ConnectBT().execute(); //Call the class to connect

        //commands to be sent to bluetooth
        On.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                turnOnLed();      //method to turn on
            }
        });

        Off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                turnOffLed();   //method to turn off
            }
        });

        Discnt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });

        hrt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                heartRate();   //method to display heart rate
            }
        });

        usg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                usage();   //method to display usage
            }
        });


    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("0".toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    public void receiveData(BluetoothSocket socket) throws IOException{
        InputStream socketInputStream =  socket.getInputStream();
        byte[] buffer = new byte[256];
        int bytes;

        // Keep looping to listen for received messages
        while (true) {
            try {
                bytes = socketInputStream.read(buffer);            //read bytes from input buffer
                String readMessage = new String(buffer, 0, bytes);
                // Send the obtained bytes to the UI Activity via handler
                Log.i("logging", readMessage + "");
                msg(readMessage);
            } catch (IOException e) {
                msg("Error");
            }
        }

    }
    private void turnOnLed()
    {

        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("1".toString().getBytes());



            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void heartRate()
    {
        byte[] buffer = new byte[256];
        int bytes;
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("!".toString().getBytes());
                bytes = btSocket.getInputStream().read(buffer)  ;
                String readMessage = new String(buffer, 0, bytes);
                Pattern pattern = Pattern.compile("BPM:(.*?):");
                Matcher matcher = pattern.matcher(readMessage);
                if (matcher.find()) {

                    Log.i("logging", matcher.group(1) + "");
                    hrtDisp.setText(matcher.group(1));

                }

            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void usage()
    {
        byte[] buffer = new byte[256];
        int bytes;
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("k".toString().getBytes());
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 200ms
                    }
                }, 200);
                bytes = btSocket.getInputStream().read(buffer)  ;
                String readMessage = new String(buffer, 0, bytes);
                Pattern pattern = Pattern.compile("TIME:(.*?):");
                Matcher matcher = pattern.matcher(readMessage);
                if (matcher.find()) {

                    Log.i("logging", matcher.group(1) + "");
                    usgDisp.setText(matcher.group(1));

                    if(Float.parseFloat(matcher.group(1))>=10)
                        msg("Please take a Break!!!!!!");


                }



            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(Control.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                 myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                 BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                 btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                 BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                 btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}
