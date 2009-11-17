package helpers;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import javax.swing.filechooser.FileSystemView;

/**
 * Global Helpers that are useful for everyone..
 * 
 * @author Quicksilver
 *
 */
public final class GH {

	private static final Random rand = new Random();
	
	private static final FileSystemView chooser = FileSystemView.getFileSystemView();
	
	/*
    *
    * @see java.util.Random#nextInt(int)
    */
	public static int nextInt(int n) {
		synchronized(rand) {
			return rand.nextInt(n);
		}
	}
	public static int nextInt() {
		synchronized(rand) {
			return rand.nextInt();
		}
	}
	
	
	
	
	private GH() {}
	
	
	/**
	 * provided for all the might use it..
	 * as creating costs memory that is never recovered..
	 * @return a FileSytemView instance..
	 */
	public static FileSystemView getFileSystemView() {
		return chooser;
	}
	
	/**
	 * emulates behaviour of FileSystemView as good as possible...
	 * though its not the same..
	 *  -> done as normal FileSystemView has a memory leak on that method..
	 */
	public static File[] getFiles(File parent,boolean useHidden) {
		File[] files = parent.listFiles();
		if (files == null) return new File[0];
			
		if (useHidden) {
		//	int length = files.length;
			int k = 0;
			for (int i = 0; i+k < files.length; i++) {
				while (i+k < files.length && files[i+k].isHidden()) {
					k++;
				} 
				if (i+k < files.length) {
					files[i] = files[i+k];	
				}
			}
			
			if (k != 0) {
				File[] onlyVisible = new File[files.length - k];
				System.arraycopy(files, 0, onlyVisible, 0, onlyVisible.length );
				return onlyVisible;
			}
		}
		return files;
	}
	
	/**
	 * closes all provided streams ignoring exceptions
	 * @param closeable
	 */
	public static void close(Closeable... closeable) {
		for (Closeable c: closeable) {
			try {
				if (c != null) {
					c.close();
				}
			} catch (IOException e) {
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @return A List of All network interfaces that can be used to bind ipv4 sockets to...
	 * @throws SocketException if getting addresses from machine fails...
	 */
	public static List<NetworkInterface> getFilteredNIList() throws SocketException {
		
		List<NetworkInterface> nic = new ArrayList<NetworkInterface>();
		
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		
		while(e.hasMoreElements()) {
			NetworkInterface ni = e.nextElement();
			Enumeration<InetAddress> e2 = ni.getInetAddresses();
			boolean use = false;
			while (e2.hasMoreElements()) {
				InetAddress ia = e2.nextElement();
				if (!ia.isLoopbackAddress() && ia instanceof Inet4Address) {
					use = true;
				}
			}
			if (use) {
				nic.add(ni);
			}
		}
		return nic;
	}
	
	/**
	 * sleeps ignoring interruption handling..
	 * 
	 * @param millis - how long to sleep
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException ie) {}
	}
	
	/**
	 * replaces invalid characters in Filenames
	 */
	public static String replaceInvalidFilename(String filename) {

		filename = filename.replace("\\", ".");
		filename = filename.replace("/", ".");
		filename = filename.replace("*", ".");
		filename = filename.replace("?", ".");
		filename = filename.replace("\"", "\'");
		filename = filename.replace("<", ".");
		filename = filename.replace(">", ".");
		filename = filename.replace("|", ".");
		filename = filename.replace(":", "-");  
		for (int i = 0; i < filename.length();i++) {
			if (filename.charAt(i) < 20) {
				filename = filename.substring(0, i)+filename.substring(i+1);
			}
		}

		return filename;
	}
	
	/**
	 * replaces invalid filepath 
	 */
	public static String replaceInvalidFilpath(String filename) {
		
		filename = filename.replace(File.separator.equals("\\")?"/": "\\", ".");
		filename = filename.replace("*", ".");
		filename = filename.replace("?", ".");
		filename = filename.replace("\"", "\'");
		filename = filename.replace("<", ".");
		filename = filename.replace(">", ".");
		filename = filename.replace("|", ".");
		filename = filename.replace(":", "-");    

		return filename;
	}
	
	/**
	 * 
	 * @param filename name of the file
	 * @return ending without the .  hello.pdf -> pdf
	 * empty String if none determined..
	 */
	public static String getFileEnding(String filename) {
		int i= filename.lastIndexOf('.');
		if(i != -1) {
			return filename.substring(i+1);
		} else {
			return "";
		}	
	}
	
	
	/**
	 * replaces newline characters with \n
	 * and \ with \\ 
	 * 
	 * so it does basic escaping
	 * 
	 * @param s
	 * @return
	 */
	public static String replaces(String s) {
		s = s.replace("\\", "\\\\");
		s = s.replace("\n", "\\n");
		return s;
	}
	
	/**
	 * reverses replacements of replace function
	 */
	public static String revReplace(String s) {
		int i = 0;
		while ((i = s.indexOf('\\',i)) != -1) {
			if (s.length() > i+1) {
				char c = s.charAt(i+1);
				if (c == '\\') {
					s = s.substring(0, i)+"\\"+s.substring(i+2) ;
				} else if (c == 'n') {
					s = s.substring(0, i)+"\n"+s.substring(i+2);
				}
			}
			i++;
		}
		
		return s;
	}
	
	public static boolean isLocaladdress(InetAddress ia) {
		return ia.isLoopbackAddress()|| ia.isSiteLocalAddress();
	}
	
	
/*	public static String getStacktrace(Thread t) {
		String s = t.getName();
		for (StackTraceElement ste:t.getStackTrace()) {
			s += "\n"+ste.getFileName()+" Line:"+ste.getLineNumber()+" "+ste.getMethodName();
		}
		return s;
	} */
	
	public static boolean isEmpty(String s) {
		return s.length() == 0;
	}
	
	public static boolean isNullOrEmpty(String s) {
		return s == null || s.length() == 0;
	}
	
	public static String toString(Object[] arr) {
		if (arr == null) {
			return "null";
		}
		if (arr.length == 0) {
			return "{empty}";
		}
		String s = "";
		for (Object o:arr) {
			s+=","+o.toString();
		}
		return "{"+s.substring(1)+"}";
	}
	
	/**
	 * 
	 * @param toFilter what Objects should be filtered..
	 * @param mayNotContain - tested against o.toString()  
	 * @return all which toString() method did not contain mayNoContain
	 */
	public static <K> List<K> filter(List<K> toFilter,String... mayNotContain) {
		ArrayList<K> s = new ArrayList<K>();
		for (K old:toFilter) {
			boolean putIn = true;
			for (String test:mayNotContain) {
				putIn = putIn && !old.toString().contains(test);
			}
			if (putIn) {
				s.add(old);
			}
		}
		return s;
	}
	
	
	public static byte[] concatenate(byte[]... arrays) {
		int totalsize= 0;
		for (byte[] array: arrays) {
			totalsize += array.length;
		}
		byte[] all = new byte[totalsize];
		int currentpos = 0;
		for (byte[] array: arrays) {
			System.arraycopy(array, 0, all, currentpos, array.length);
			currentpos += array.length;
		}
		return all;
		
	}
	
	public static int compareTo(int a , int b) {
		return (a < b ? -1 : (a==b ? 0 : 1));
	}
	
	public static int compareTo(byte a , byte b) {
		return (a < b ? -1 : (a==b ? 0 : 1));
	}
	
	public static int unsingedCompareTo(byte a,byte b) {
		return compareTo(a & 0xff , b & 0xff);
	}
	
	/**
	 * concatenates  each term in collection using .toString()
	 * and puts between each string "between" 
	 * 
	 * if the map is empty it will return the empty map string instead...
	 * 
	 * prefix and postfix are applied around everything..
	 */
	public static String concat(Collection<?> terms,String between,String emptyMap) {
		String ret = null;
		
		for (Object o: terms) {
			if (ret != null) {
				ret += between+o.toString();
			} else {
				ret  = o.toString();
			}
		}
		if (ret == null) {
			return emptyMap;
		} else {
			return ret;
		}
	}
	
	
	/**
	 * 
	 * @return a string representing all stacktraces..
	 */
	public static String getAllStackTraces() {
		ThreadMXBean threadBean =
	        ManagementFactory.getThreadMXBean(); 
		
		String newLine = System.getProperty("line.separator");
		String s = "";
		for (Entry<Thread,StackTraceElement[]> e :Thread.getAllStackTraces().entrySet()) {
			s += newLine+newLine;
			Thread d = e.getKey();
			ThreadInfo ti = threadBean.getThreadInfo(d.getId());
			
			s += String.format("%s , state: %s ,id: %s",d.toString(),d.getState().toString(),d.getId())+newLine;
			String lock= ti.getLockName();
			if (lock != null) {
				s += String.format("Lock: %s, Count: %s,Time: %s, Owner: %s ", 
						lock,ti.getBlockedCount(),ti.getBlockedTime(),ti.getLockOwnerId());
				s += newLine;
			}

			s += concat(Arrays.asList(e.getValue()),newLine,"empty");
		}
		return s+newLine;
	}

	
	/**
	 * 
	 * @param s the string to search 
	 * @param what what char to be searched
	 * @return the number of occurences of what in s
	 */
	public static int getOccurences(String s , char what) {
		int count = 0,pos = 0;
		
		while (-1 != (pos = s.indexOf(what, pos))) {
			count++;
			pos++;
		}
		
		return count;
	}
}
