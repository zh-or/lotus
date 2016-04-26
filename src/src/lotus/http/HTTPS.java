package lotus.http;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HTTPS {
    
    /**
     * 上传文件
     * @param url_
     * @param file
     * @param filetype
     * @return
     */
	public static String uploadFile(String url_, File file, String filetype) {
			String msg = null;
			String end = "\r\n";
			String twoHyphens = "--";
			String boundary = "**----what-the-fuck**";
			try {
				URL url = new URL(url_);
				HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
				/* 允许Input、Output，不使用Cache */
				SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
				con.setSSLSocketFactory(sc.getSocketFactory());
			    con.setHostnameVerifier(new TrustAnyHostnameVerifier());
				con.setDoInput(true);
				con.setDoOutput(true);
				con.setUseCaches(false);
				con.setConnectTimeout(5000);
				con.setReadTimeout(10000);
				/* 设置传送的method=POST */
				con.setRequestMethod("POST");
				/* setRequestProperty */
				con.setRequestProperty("Connection", "Keep-Alive");
				con.setRequestProperty("Charset", "UTF-8");
				con.setRequestProperty("Content-Type","multipart/form-data;boundary=" + boundary);
				/* 设置DataOutputStream */
				DataOutputStream ds = new DataOutputStream(con.getOutputStream());
				ds.writeBytes(twoHyphens + boundary + end);
				ds.writeBytes("Content-Disposition: form-data; "+ "name=\"" + file.getName() + "\";filename=\"" + file.getName() + "\"" + end);
				ds.writeBytes("Content-Type: " + filetype + end);
				ds.writeBytes(end);
				

				FileInputStream fStream = new FileInputStream(file);
			
				byte[] buffer = new byte[1024];
				int length = -1;
				while ((length = fStream.read(buffer)) != -1) {
					ds.write(buffer, 0, length);
				}
				ds.writeBytes(end);
				ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
				/* close streams */
				fStream.close();
				ds.flush();
				/* 取得Response内容 */
				InputStream is = con.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				int lengthi = -1;
				while ((lengthi = is.read(buf)) != -1) {
					baos.write(buf, 0, lengthi);
				}
				msg = new String(baos.toByteArray(), "UTF-8");
				ds.close();
//				Log.i(StaticVariable.TAG, msg);
				return msg;

			} catch (Exception e) {
				e.printStackTrace();
				return "";
			}
		}
	
	/**
	 * urlencoded
	 * @param url
	 * @param content
	 * @return
	 */
	public static String post(String url, String content) {
		OutputStream out = null;
		InputStream in = null;
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
	 
	        URL console = new URL(url);
	        HttpsURLConnection conn = (HttpsURLConnection) console.openConnection();
	        conn.setSSLSocketFactory(sc.getSocketFactory());
	        conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	        conn.setRequestProperty("Charset", "UTF-8");
	        conn.setRequestMethod("POST");
	        conn.setUseCaches(false);
	        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        conn.connect();
	        out = conn.getOutputStream();
	        out.write(content.getBytes("UTF-8"));
	        // 刷新、关闭
	        out.flush();
	        out.close();
	        out = null;
	        in = conn.getInputStream();
	        if (in != null) {
	            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	            byte[] buffer = new byte[1024 * 2];
	            int len = 0;
	            while ((len = in.read(buffer)) != -1) {
	                outStream.write(buffer, 0, len);
	            }
	            in.close();
	            in = null;
	            return new String(outStream.toByteArray(), "UTF-8");
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(in != null) in.close();
				if(out != null) out.close();
			} catch (Exception e2) {}
		}
        return "";
	}
	
	
	public static String get(String url){
		InputStream is = null;
		ByteArrayOutputStream outStream = null;
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
	 
	        URL console = new URL(url);
	        HttpsURLConnection conn = (HttpsURLConnection) console.openConnection();
	        conn.setSSLSocketFactory(sc.getSocketFactory());
	        conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
	        conn.setDoInput(true);
	        conn.setRequestProperty("Charset", "UTF-8");
	        conn.setRequestMethod("GET");
	        conn.setUseCaches(false);
	        conn.connect();
	      
	        is = conn.getInputStream();
	        if (is != null) {
	            outStream = new ByteArrayOutputStream();
	            byte[] buffer = new byte[1024];
	            int len = 0;
	            while ((len = is.read(buffer)) != -1) {
	                outStream.write(buffer, 0, len);
	            }
	            is.close();
	            return new String(outStream.toByteArray(), "UTF-8");
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(is != null) is.close();
				if(outStream != null) outStream.close();
			} catch (Exception e2) {}
		}
        return "";
	}
	
	/**
	 * 普通http请求
	 * @param url
	 * @return
	 */
	public static String _get(String url){
		InputStream is = null;
		ByteArrayOutputStream outStream = null;
		try {
	        URL console = new URL(url);
	        HttpURLConnection conn = (HttpURLConnection) console.openConnection();
	        conn.setDoInput(true);
	        conn.setRequestProperty("Charset", "UTF-8");
	        conn.setRequestMethod("GET");
	        conn.setUseCaches(false);
	        conn.connect();
	      
	        is = conn.getInputStream();
	        if (is != null) {
	            outStream = new ByteArrayOutputStream();
	            byte[] buffer = new byte[2048];
	            int len = 0;
	            while ((len = is.read(buffer)) != -1) {
	                outStream.write(buffer, 0, len);
	            }
	            is.close();
	            return new String(outStream.toByteArray(), "UTF-8");
	        }
		} catch (Exception e) {
		}finally{
			try {
				if(is != null) is.close();
				if(outStream != null) outStream.close();
			} catch (Exception e2) {}
		}
        return "";
	}
	
	private static class TrustAnyTrustManager implements X509TrustManager {
		 
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
 
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
 
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
	}
 
	private static class TrustAnyHostnameVerifier implements HostnameVerifier {
	        public boolean verify(String hostname, SSLSession session) {
	            return true;
	        }
	}
	
}
