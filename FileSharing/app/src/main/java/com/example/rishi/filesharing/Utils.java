package com.example.rishi.filesharing;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;

public class Utils {

    



    private final static String p2pInt = "p2p-p2p0";

    public String readFromSocket(Socket socket) throws Exception {

        String response = "";
        try {

            // publishProgress("after socket in client");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
                    10000);
            // publishProgress("after byte array socket in client");
            byte buffer[] = new byte[10000];
            // publishProgress("after new byte[1024] in client");

            InputStream inputStream = socket.getInputStream();

            while (true) {
                // publishProgress("after inputstream socket in client");
                if (inputStream.available() == 0) {
                    continue;
                }
                int bytesRead = -1;
                // publishProgress("before do in socket in client");
                do {
                    bytesRead = inputStream.read(buffer);
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                    response += byteArrayOutputStream.toString("UTF-8");
                    // publishProgress("Publishing responses...");

                    // publishProgress(response);
                    bytesRead = -1;
                } while (bytesRead != -1);
                break;

            }
            // publishProgress("after loop socket in client");
        } catch (Exception e) {
            e.printStackTrace();
            //MainActivity.addLog(e.toString());
            response = "Exception " + e.toString();
        }

        return response;

    }

    public void writeToSocket(Socket socket, String data) throws Exception {
        int size = data.length();
        byte arr[] = new byte[size];
        arr = data.getBytes();
        OutputStream outputStream = socket.getOutputStream();
        // publishProgress("Stream ready to write");
        outputStream.write(arr, 0, arr.length);

        // publishProgress("Written from my side. Baaki doosre ka dekh lo");
    }

    public static String getIPFromMac(String MAC) {
		/*
		 * http://www.flattermann.net/2011/02/android-howto-find-the-hardware-mac-address-of-a-remote-host/
		 *
		 * */
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {

                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    // Basic sanity check
                    String device = splitted[5];
                    if (device.matches(".*" +p2pInt+ ".*")){
                        String mac = splitted[3];
                        if (mac.matches(MAC)) {
                            return splitted[0];
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static String getLocalIPAddress() {
		/*
		 * http://thinkandroid.wordpress.com/2010/03/27/incorporating-socket-programming-into-your-applications/
		 *
		 * */
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();

                    String iface = intf.getName();
                    if(iface.matches(".*" +p2pInt+ ".*")){
                        if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return getDottedDecimalIP(inetAddress.getAddress());
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        } catch (NullPointerException ex) {
            Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        }
        return null;
    }

    private static String getDottedDecimalIP(byte[] ipAddr) {
		/*
		 * http://stackoverflow.com/questions/10053385/how-to-get-each-devices-ip-address-in-wifi-direct-scenario
		 *
		 * */
        String ipAddrStr = "";
        for (int i=0; i<ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr += ".";
            }
            ipAddrStr += ipAddr[i]&0xFF;
        }
        return ipAddrStr;
    }



    public static void downloadFromSite(String url, String path) throws MalformedURLException {
        try {
            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(path + "/" + "information");
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }



}
