package lotus.config;	

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 简单ini配置文件读取
 * @author OR
 */
public class Config {
	
	private HashMap<String, HashMap<String, String>> group = null;
	private File file;

	public Config(File f){
		group = new HashMap<String, HashMap<String,String>>();
		this.file = f;
	}
	
	public void read(){
	    read("UTF-8");
	}
	
	/**
	 * 读取配置文件
	 */
	public void read(String charset){//
		BufferedReader bis = null;
		try {
			bis = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
			HashMap<String, String> child = null;
			String line = null;
			while (null != (line = bis.readLine())){
			//	System.out.println("line:" + line);
				if('[' == line.charAt(0) && ']' == line.charAt(line.length() - 1)){
					child  = new HashMap<String, String>();
					child.clear();
					String gname = line.substring(1, line.length() - 1);
					group.put(gname, child);
				}else if(';' != line.charAt(0) && child != null){//是否注释
					String cname = "", cvalue = "";
					int p = line.indexOf("=");
					if(p == -1) continue;
					cname = line.substring(0, p);
					cvalue = line.substring(p + 1, line.length());
					cname = cname.trim();
					cvalue = cvalue.trim();
					child.put(cname, cvalue);
				}
				
			}
		} catch (Exception e) {
			
		}finally{
			try {
				if(bis != null) bis.close();
			} catch (Exception e2) {
			}
		}
		
		
	}

	/**
	 * 读取配置
	 * @param groupkey
	 * @param childkey 
	 * @param defaultvalue 默认值
	 * @return
	 */
	public String getStringValue(String groupkey, String childkey, String defaultvalue){
		HashMap<String, String>  child = group.get(groupkey);
		if(child == null) return defaultvalue;
		String v = child.get(childkey);
		return (v == null || "".equals(v)) ? defaultvalue : v;
	}
	
	public int getIntValue(String groupkey, String childkey, int defaultvalue){
		return Integer.valueOf(getStringValue(groupkey, childkey, defaultvalue + ""));
	}
	
	/**
	 * @param groupkey
	 * @param childkey
	 * @param defaultvalue
	 * @return 当此配置项的值为 'true' 或 '1' 时返回 true
	 */
	public boolean getBoolValue(String groupkey, String childkey, boolean defaultvalue){
		String v = getStringValue(groupkey, childkey, defaultvalue + "");
		return "true".equals(v) || "1".equals(v);
	}
	
	
	/**
	 * 设置值 如果有则覆盖 没有则创建
	 * @param groupkey
	 * @param childkey
	 * @param value
	 * @return
	 */
	public String putValue(String groupkey, String childkey, String value){
		HashMap<String, String>  child = group.get(groupkey);
		if(child == null){
			child = new HashMap<String, String>();
			child.put(childkey, value);
			group.put(groupkey, child);
			return null;
		}
		return child.put(childkey, value);
	}
	
	public void save(){
		
		FileWriter fout = null;
		try {
			StringBuilder sb = new StringBuilder();
			fout = new FileWriter(file);
			Iterator<?> iter = group.entrySet().iterator();
			while (iter.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry entry = (Map.Entry) iter.next();
				String key = (String) entry.getKey();
				@SuppressWarnings("unchecked")
				HashMap<String, String> val = (HashMap<String, String>) entry.getValue();
				sb.append("[" + key + "]\n");
				Iterator<?> vs = val.entrySet().iterator();
				while(vs.hasNext()){
					@SuppressWarnings("rawtypes")
					Map.Entry c_entry = (Map.Entry) vs.next();
					String k = (String) c_entry.getKey();
					String v = (String) c_entry.getValue();
					sb.append(k + "=" + v + "\n");
				}
			}
			fout.write(sb.toString());
			fout.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				if(fout != null) fout.close();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		return "Config [group=" + group + "]";
	}
	
}