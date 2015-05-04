package com.example.android.wifidirect;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Writer;
import java.lang.System;
import java.net.HttpURLConnection;
import android.util.Log;

public class Utils {
	public class Peer{
		public String IP;
		public int assignedChunk;
		public int timeout;
	}
	private final static String p2pInt = "p2p-p2p0";
	public static int HistWeight=0.8; //This variable is used to determine the weightage of timeout
	
	public static String getIPFromMac(String MAC) {
		/*
		 * method modified from:
		 * 
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
	
	
	/*
	 * Returns size of file in bytes at url 
	 */
	public static int getFileSize(URL url){
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.connect();
		return connection.getContentLength();
	}
	
	/*
	 * Given an ArrayList of peers, assigns to every free peer an unreceived chunk
	 * n : no. of chunks
	 * assigned[i] is true iff ith chunk has been assigned
	 * received[i] is true iff ith chunk has been received
	 * peers : The list of peers in our group
	 * returns the list of peers that have been newly assigned chunks
	 */
	public static ArrayList<Peer> assignChunks(int n, bool[] assigned, bool[] received, ArrayList<Peer> peers){
		int l=peers.size();
		ArrayList<Peer> changes = new ArrayList()
		for(int i=0;i<l;i++)
			if (peers[i].timeout<currentTimeMillis())
				assigned[peers[i].assignedChunk]=false;
		int j=0,i=0;
		for(j;(received[j]||assigned[j])&&j<n;j++);!received[j]&&assigned[j]
		while(i<l&&j<n){
			if (peers[i].assignedChunk=-1){
				assigned[j]=true;
				peers[i++].assignedChunk=j++;
				changes.add(peers.get(i));
			}
			for(j;(received[j]||assigned[j])&&j<n;j++);
		}
		return changes;
	}
	
	/*
	 * If the current expected download speed is x, and a the newest file was downloaded at speed y, then new expected 
	 * download speed = HistWeight*x+(1-HistWeight)*y
	 * Useful for setting timeout periods
	 */
	public static int getExpectedSpeed(int current, int latest){
		return current*HistWeight+latest*(1-HistWeight);
	}
	
	/*
     * Given URL and range of file, returns a byte array containing that chunk, start and end inclusive
     * url : The url of the file to be downloaded
     * start : starting byte position
     * end : ending byte position
     * speed : meant for returning download speed in KB/s. Speed should be a single element array and this value resides in speed[0]
     */
	public static byte[] downloadRange(URL url, int start, int end, int[] speed){
        byte []b = new byte[end-start+1];
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Range", "bytes=" + start + "-");
        connection.setDoInput(true);
        connection.setDoOutput(true);
        BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
        long s=currentTimeMillis();
        for(int i=start;i<=end;i++){
            b[i-start]=in.read();
        }
        s=s-currentTimeMillis();
        speed[0]=(end-start+1)/1.024/s;
        return b;
    }
	

	/*
	 *  Saves b into file named as <prefix><index>
	 *  b : byte array to be written into file
	 */
	public static void savetoDisc(byte[] b, int index, String prefix){
		FileOutputStream f = new FileOutputStream(new File("/sdcard/"+prefix+toString(index)));
		int n=b.length;
		for(int i=0;i<n;i++)
			f.write(b[i]);
		f.flush();
		f.close();
	}
	
	/*
	 * Stitches n files named as <prefix><i>, from <prefix>0 to prefix<n-1> into file "name". It is assumed that all these files
	 * follow a naming pattern, <prefix>0,<prefix>1...
	 * prefix : The prefix that files are
	 * name : The name of the file to be written into
	 * n : The number of such indexed files that need to be stiched
	 * size : The maximum size of any one indexed file
	 */
	public static void stitchFiles(String prefix, String name, int n, int size){
		FileOutputStream f = new FileOutputStream(new File("/sdcard/"+name));
		for(int i=0;i<n;i++){
			FileInputStream ff = new FileInputStream(new File("/sdcard/"+prefix+toString(i)));
			byte temp[size];	//
			int r=ff.read(temp);
			f.write(temp,0,r);	//write r bytes from the start of array temp into f
			ff.close();
		}
		f.flush();
		f.close();
	}
	
	public static String getLocalIPAddress() {
		/*
		 * modified from:
		 * 
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
		 * ripped from:
		 * 
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
}
