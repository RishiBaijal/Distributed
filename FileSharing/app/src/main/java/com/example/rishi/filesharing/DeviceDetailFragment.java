package com.example.rishi.filesharing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import android.util.Log;
//import com.example.android.wifidirect.DeviceListFragment.DeviceActionListener;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    public static final String IP_SERVER = "192.168.49.1";
    public static int PORT = 8080;
    private static boolean server_running = false;

    public static boolean isGroupOwner = false;
    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    private WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.fragment_device_detail, null);
        mContentView.findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                        "Connecting to :" + device.deviceAddress, true, true

                );
                ((DeviceListFragment.DeviceActionListener) getActivity()).connect(config);

            }
        });

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceListFragment.DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                    }
                });

        return mContentView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        //String localIP = Utils.getLocalIPAddress();
        WifiManager wm = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        String localIP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        // Trick to find the ip in the file /proc/net/arp
        System.out.println("The local IP address is "+localIP);
      //  Log.d("The local IP address is ",  localIP);
        String client_mac_fixed = new String(device.deviceAddress).replace("99", "19");
        String clientIP = Utils.getIPFromMac(client_mac_fixed);

        //System.out.println("The IP address of the client is "+clientIP);

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        statusText.setText("Sending: " + uri);
        Log.d(MainActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());

        if(localIP.equals(IP_SERVER)){
            serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, clientIP);
        }else{
            serviceIntent.putExtra(FileTransferService.EXTRAS_ADDRESS, IP_SERVER);
        }

        serviceIntent.putExtra(FileTransferService.EXTRAS_PORT, PORT);
        getActivity().startService(serviceIntent);
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // The owner IP is now known.
        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(getResources().getString(R.string.group_owner_text)
                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
                : getResources().getString(R.string.no)));

        // InetAddress from WifiP2pInfo struct.
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());

        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);

        if (!server_running){
            new ServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();
            server_running = true;
        }

        // hide the connect button
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        System.out.println(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    public void resetViews() {
        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.group_owner);
        view.setText(R.string.empty);
        view = (TextView) mContentView.findViewById(R.id.status_text);
        view.setText(R.string.empty);
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }




    public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {

        private final Context context;
        private final TextView statusText;

        /**
         * @param context
         * @param statusText
         */
        public ServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                Log.d(MainActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                System.out.println("The BROTHERFUCKING IP address of the client is "+client.getInetAddress());
                Log.d(MainActivity.TAG, "Server: connection done");
                Log.d(MainActivity.TAG, "The BROTHERFUCKING IP address of the client is "+client.getInetAddress());

                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(MainActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                server_running = false;
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(MainActivity.TAG, e.getMessage());
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }

        }


        @Override
        protected void onPreExecute() {
            statusText.setText("Opening a server socket");
        }

    }

//    public static boolean sendIPAddress(String IP, OutputStream out)
//    {
//
//    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(MainActivity.TAG, e.toString());
            return false;
        }
        return true;
    }

}





//package com.example.rishi.filesharing;
//
//import android.app.Activity;
//import android.app.ProgressDialog;
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//import android.net.wifi.p2p.WifiP2pDevice;
//import android.net.wifi.p2p.WifiP2pInfo;
//import android.net.wifi.p2p.WifiP2pManager;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.app.Fragment;
//import android.os.Environment;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.net.ServerSocket;
//import java.net.Socket;
//
//
///**
// * A simple {@link Fragment} subclass.
// * Activities that contain this fragment must implement the
// * {@link DeviceDetailFragment.OnFragmentInteractionListener} interface
// * to handle interaction events.
// * Use the {@link DeviceDetailFragment#newInstance} factory method to
// * create an instance of this fragment.
// */
//public class DeviceDetailFragment extends Fragment implements WifiP2pManager.ConnectionInfoListener {
//    // TODO: Rename parameter arguments, choose names that match
//    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
//    private static final String ARG_PARAM1 = "param1";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
//    private String mParam1;
//    private String mParam2;
//
//    public static final String IP_SERVER = "192.168.49.1";
//    public static int PORT = 8988;
//    private static boolean server_running = false;
//
//    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
//
//    private View mContentView = null;
//    private WifiP2pDevice device;
//    ProgressDialog progressDialog = null;
//    private WifiP2pInfo info;
//
//
//    private OnFragmentInteractionListener mListener;
//
//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment DeviceDetailFragment.
//     */
//    // TODO: Rename and change types and number of parameters
//    public static DeviceDetailFragment newInstance(String param1, String param2) {
//        DeviceDetailFragment fragment = new DeviceDetailFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
//        return fragment;
//    }
//
//    public DeviceDetailFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_device_detail, container, false);
//    }
//
//    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
//    }
//
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }
//
//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     * <p/>
//     * See the Android Training lesson <a href=
//     * "http://developer.android.com/training/basics/fragments/communicating.html"
//     * >Communicating with Other Fragments</a> for more information.
//     *
//     *
//     */
//
//    @Override
//    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
//        if (progressDialog != null && progressDialog.isShowing()) {
//            progressDialog.dismiss();
//        }
//        this.info = info;
//        this.getView().setVisibility(View.VISIBLE);
//
//        // The owner IP is now known.
//        TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
//        view.setText(getResources().getString(R.string.group_owner_text)
//                + ((info.isGroupOwner == true) ? getResources().getString(R.string.yes)
//                : getResources().getString(R.string.no)));
//
//        // InetAddress from WifiP2pInfo struct.
//        view = (TextView) mContentView.findViewById(R.id.device_info);
//        view.setText("Group Owner IP - " + info.groupOwnerAddress.getHostAddress());
//
//        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
//
//        if (!server_running){
//            new ServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text)).execute();
//            server_running = true;
//        }
//
//        // hide the connect button
//        mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
//    }
//
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        public void onFragmentInteraction(Uri uri);
//    }
//
//
//    public void showDetails(WifiP2pDevice device) {
//        this.device = device;
//        this.getView().setVisibility(View.VISIBLE);
//        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
//        view.setText(device.deviceAddress);
//        view = (TextView) mContentView.findViewById(R.id.device_info);
//        view.setText(device.toString());
//
//    }
//
//    /**
//     * Clears the UI fields after a disconnect or direct mode disable operation.
//     */
//    public void resetViews() {
//        mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
//        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
//        view.setText(R.string.empty);
//        view = (TextView) mContentView.findViewById(R.id.device_info);
//        view.setText(R.string.empty);
//        view = (TextView) mContentView.findViewById(R.id.group_owner);
//        view.setText(R.string.empty);
//        view = (TextView) mContentView.findViewById(R.id.status_text);
//        view.setText(R.string.empty);
//        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
//        this.getView().setVisibility(View.GONE);
//    }
//
//    /**
//     * A simple server socket that accepts connection and writes some data on
//     * the stream.
//     */
//    public static class ServerAsyncTask extends AsyncTask<Void, Void, String> {
//
//        private final Context context;
//        private final TextView statusText;
//
//        /**
//         * @param context
//         * @param statusText
//         */
//        public ServerAsyncTask(Context context, View statusText) {
//            this.context = context;
//            this.statusText = (TextView) statusText;
//        }
//
//        @Override
//        protected String doInBackground(Void... params) {
//            try {
//                ServerSocket serverSocket = new ServerSocket(PORT);
//                Log.d(MainActivity.TAG, "Server: Socket opened");
//                Socket client = serverSocket.accept();
//                Log.d(MainActivity.TAG, "Server: connection done");
//                final File f = new File(Environment.getExternalStorageDirectory() + "/"
//                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
//                        + ".jpg");
//
//                File dirs = new File(f.getParent());
//                if (!dirs.exists())
//                    dirs.mkdirs();
//                f.createNewFile();
//
//                Log.d(MainActivity.TAG, "server: copying files " + f.toString());
//                InputStream inputstream = client.getInputStream();
//                copyFile(inputstream, new FileOutputStream(f));
//                serverSocket.close();
//                server_running = false;
//                return f.getAbsolutePath();
//            } catch (IOException e) {
//                Log.e(MainActivity.TAG, e.getMessage());
//                return null;
//            }
//        }
//
//        /*
//         * (non-Javadoc)
//         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
//         */
//        @Override
//        protected void onPostExecute(String result) {
//            if (result != null) {
//                statusText.setText("File copied - " + result);
//                Intent intent = new Intent();
//                intent.setAction(android.content.Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
//                context.startActivity(intent);
//            }
//
//        }
//
//        /*
//         * (non-Javadoc)
//         * @see android.os.AsyncTask#onPreExecute()
//         */
//        @Override
//        protected void onPreExecute() {
//            statusText.setText("Opening a server socket");
//        }
//
//    }
//
//
//
//    public static boolean copyFile(InputStream inputStream, OutputStream out) {
//        byte buf[] = new byte[1024];
//        int len;
//        try {
//            while ((len = inputStream.read(buf)) != -1) {
//                out.write(buf, 0, len);
//
//            }
//            out.close();
//            inputStream.close();
//        } catch (IOException e) {
//            Log.d(MainActivity.TAG, e.toString());
//            return false;
//        }
//        return true;
//    }
//
//}



