package com.example.android.wifidirect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.os.Environment;
import android.util.Log;

import static java.lang.System.currentTimeMillis;

public class Utils {
	public class Peer{
		public String IP;
		public int assignedChunk;
		public int timeout;
	}
	private final static String p2pInt = "p2p-p2p0";
	public static double HistWeight=0.8; //This variable is used to determine the weightage of timeout

	public static boolean createInitDir() {
		try {
			File myDirectory = new File("/storage/emulated/0/WiFiP2p_chunks");
			if (!myDirectory.exists()) {
				myDirectory.mkdirs();
			}
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

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

	public static int getFileSize(URL url) throws  IOException{
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.connect();
		return connection.getContentLength();
	}

	/*
	 * Given an ArrayList of peers, assigns to every free peer an unreceived chunk
	 * n : no. of chunks
	 * received[i] is true iff ith chunk has been received/assigned
	 * peers : The list of peers in our group
	 */
	/*public static ArrayList<Peer> assignChunks(int n, boolean[] received, ArrayList<Peer> peers){
		int l=peers.size();
		ArrayList<Peer> changes = new ArrayList();
		for(int i=0;i<l;i++)
			if (peers.get(i).timeout<currentTimeMillis())
				received[peers.get(i).assignedChunk]=false;
		int j=0,i=0;
		for(;received[j]&&j<n;j++);
		while(i<l&&j<n){
			if (peers[i].assignedChunk=-1){
				peers[i++].assignedChunk=j++;
				changes.add(peers.get(i));
			}
			for(;received[j]&&j<n;j++);
		}
		return changes;
	}*/

	/*
	 * If the current expected download speed is x, and a the newest file was downloaded at speed y, then new expected 
	 * download speed = HistWeight*x+(1-HistWeight)*y
	 * Useful for setting timeout periods
	 */
	public static double getExpectedSpeed(int current, int latest){
		return current*HistWeight+latest*(1-HistWeight);
	}

	/*
     * Given URL and range of file, returns a byte array containing that chunk, start and end inclusive
     * url : The url of the file to be downloaded
     * start : starting byte position
     * end : ending byte position
     * speed : meant for returning download speed in KB/s. Speed should be a single element array and this value resides in speed[0]
     */
	public static byte[] downloadRange(URL url, int start, int end, int[] speed) throws IOException{
		byte []b = new byte[end-start+1];
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestProperty("Range", "bytes=" + start + "-");
		connection.setDoInput(true);
		connection.setDoOutput(true);
		BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
		long s=currentTimeMillis();
		for(int i=start;i<=end;i++){
			b[i-start]= (byte) in.read();
		}
		s=s-currentTimeMillis();
		speed[0]= (int) ((end-start+1)/1.024/s);
		return b;
	}


	/*
	 *  Saves b into file named as <prefix><index>
	 *  b : byte array to be written into file
	 */
	public static String savetoDisc(byte[] b, int index, String prefix) throws IOException {
		String file_out = "/sdcard/WiFiP2p_chunks/" + prefix + Integer.toString(index);
		FileOutputStream f = new FileOutputStream(new File(file_out));
		int n = b.length;
		for (int i = 0; i < n; i++)
			f.write(b[i]);
		f.flush();
		f.close();
		return file_out;
	}
	 /* n : The number of such indexed files that need to be stiched
	 * size : The maximum size of any one indexed file
	 */
	public static void stitchFiles(String prefix, String name, int n, int size) throws IOException{
		FileOutputStream f = new FileOutputStream(new File("/sdcard/"+name));
		for(int i=0;i<n;i++){
			FileInputStream ff = new FileInputStream(new File("/sdcard/"+prefix+Integer.toString(i)));
			byte temp[] = new byte[size];	//
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
			Log.e("AndroidNetworkFactory", "getLocalIPAddress()", ex);
		} catch (NullPointerException ex) {
			Log.e("AndroidNetworkFactory", "getLocalIPAddress()", ex);
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
