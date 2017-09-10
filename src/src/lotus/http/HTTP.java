package lotus.http;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HTTP {
    
    public static boolean downloadFile(String url, String savepath, String cookie){
        InputStream is = null;
        FileOutputStream fout = null;
        
        try {
     
            URL console = new URL(url);
            URLConnection connection = console.openConnection();
            if(connection instanceof HttpsURLConnection){
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
                HttpsURLConnection conn = (HttpsURLConnection) connection;
                conn.setSSLSocketFactory(sc.getSocketFactory());
                conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
                conn.setRequestMethod("GET");
            }else{
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
            
            connection.setDoInput(true);
            connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Cookie", cookie);
            connection.setUseCaches(false);
            connection.connect();
            if(connection instanceof HttpsURLConnection){
                HttpsURLConnection conn = (HttpsURLConnection) connection;
                if(conn.getResponseCode() != 200){
                    return false;
                }
            }else{
                HttpURLConnection conn = ((HttpURLConnection) connection);
                if(conn.getResponseCode() != 200){
                    return false;
                }
            }
            try {
                File out = new File(savepath);
                if(out.exists()) out.delete();
                
                out.createNewFile();
                fout = new FileOutputStream(out);
            } catch (Exception e) {
                return false;
            }
            
            is = connection.getInputStream();
            if (is != null) {
                int len = 0;
                byte[] buffer = new byte[4096];
                while ((len = is.read(buffer)) != -1) {
                    fout.write(buffer, 0, len);
                }
                fout.flush();
                fout.close();
                is.close();
                return true;
            }
        } catch (Exception e) {
          //  e.printStackTrace();
        }finally{
            try {
                if(is != null) is.close();
            } catch (Exception e2) {}
        }
        return false;
    }
    
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
			
				byte[] buffer = new byte[2048];
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
				byte[] buf = new byte[2048];
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
	
	public static String post(String url, HashMap<String, String> args, String cookie){
		StringBuffer content = new StringBuffer();
		try {
			content.append("?");
			for(String key : args.keySet()){
				content.append(key);
				content.append("=");
				content.append(URLEncoder.encode(args.get(key), "utf-8"));
				content.append("&");
			}
			content.deleteCharAt(content.length() - 1);
			return post(url, content.toString(), cookie, "application/x-www-form-urlencoded");
		} catch (Exception e) {
			
		}
		return "";
	}
	
	/**
	 * urlencoded
	 * @param url
	 * @param content application/x-www-form-urlencoded
	 * @return
	 */
	public static String post(String url, String content, String cookie, String contentType) {
		OutputStream out = null;
		InputStream in = null;
		try {
	 
	        URL console = new URL(url);
	        URLConnection connection = console.openConnection();
	        connection.setConnectTimeout(60 * 1000);
	        connection.setReadTimeout(60 * 1000);
	        
    	    if(connection instanceof HttpsURLConnection){
    			SSLContext sc = SSLContext.getInstance("SSL");
    	        sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
	            HttpsURLConnection conn = (HttpsURLConnection) connection;
	            conn.setSSLSocketFactory(sc.getSocketFactory());
	            conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
	            conn.setRequestMethod("POST");
	        }else{
	            
                ((HttpURLConnection) connection).setRequestMethod("POST");
            }
	        connection.setRequestProperty("Charset", "UTF-8");
            connection.setUseCaches(false);
            connection.setRequestProperty("Cookie", cookie);
	        connection.setDoInput(true);
	        connection.setDoOutput(true);
	        connection.setRequestProperty("Content-Type", contentType); //"application/x-www-form-urlencoded");
	        byte[] datas = content.getBytes("UTF-8");
	        connection.setRequestProperty("Content-Length", datas.length + "");
	        connection.connect();
	        out = connection.getOutputStream();
	        out.write(datas);
	        // 刷新、关闭
	        out.flush();
	        in = connection.getInputStream();
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
	
	
	public static String get(String url, String cookie){
		InputStream is = null;
		ByteArrayOutputStream outStream = null;
		try {
	 
	        URL console = new URL(url);
	        URLConnection connection = console.openConnection();
	        if(connection instanceof HttpsURLConnection){
    			SSLContext sc = SSLContext.getInstance("SSL");
    	        sc.init(null, new TrustManager[] { new TrustAnyTrustManager() },  new java.security.SecureRandom());
	            HttpsURLConnection conn = (HttpsURLConnection) connection;
	            conn.setSSLSocketFactory(sc.getSocketFactory());
	            conn.setHostnameVerifier(new TrustAnyHostnameVerifier());
	            conn.setRequestMethod("GET");
	        }else{
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
	        
	        connection.setDoInput(true);
	        connection.setRequestProperty("Charset", "UTF-8");
            connection.setRequestProperty("Cookie", cookie);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36");
	        connection.setUseCaches(false);
	        connection.connect();
	      
	        is = connection.getInputStream();
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
