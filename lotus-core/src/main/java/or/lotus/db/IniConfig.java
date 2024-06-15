package or.lotus.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;


/**
 * 简单ini配置文件读取
 * @author OR
 */
public class IniConfig {

	private HashMap<String, HashMap<String, ArrStringValue>> group = null;
	private File file = null;

	public class ArrStringValue{
	    private ArrayList<String> val;

	    public ArrStringValue(){
	        val = new ArrayList<String>();
	    }

	    public ArrStringValue put(String v){
	        val.add(v);
	        return this;
	    }

	    public ArrStringValue set(String v){
	        val.clear();
	        val.add(v);
	        return this;
	    }

	    public ArrayList<String> getVal(){
	        return val;
	    }

	    public int size() {
	        return val.size();
	    }

        @Override
        public String toString() {
            if(val.size() > 0){
                return val.get(0);
            }
            return null;
        }

	}

	public IniConfig() {
	    this(null);
	}

	public IniConfig(File f) {
		group = new HashMap<String, HashMap<String, ArrStringValue>>();
		this.file = f;
	}

	public  void read() {
	    read("UTF-8");
	}

	/**
	 * 读取配置文件
	 */
	public synchronized void read(String charset) {
	    if(file == null || !file.exists()) {
	        return;
	    }
		BufferedReader bis = null;
		try {
			bis = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
            group.clear();
			HashMap<String, ArrStringValue> child = null;
			String line = null;
			while (null != (line = bis.readLine())){
			//	System.out.println("line:" + line);
				line = line.trim();
				if(line.length() <= 0) {
				    //空行
				    continue;
				}
				if('[' == line.charAt(0) && ']' == line.charAt(line.length() - 1)){
					child  = new HashMap<String, ArrStringValue>();
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
					ArrStringValue v = child.get(cname);
					if(v != null){//如果这个已经存在
					    v.put(cvalue);
					}else{
					    child.put(cname, new ArrStringValue().put(cvalue));
					}

				}

			}
		} catch (Exception e) {

		}finally{
			try {
				if(bis != null) bis.close();
				bis = null;
			} catch (Exception e2) {
			}
		}
	}

	public String[] keys() {
	    int b = 0;
	    if(group.size() <= 0) return new String[0];
	    String[] keys = new String[group.size()];
	    Iterator<Entry<String, HashMap<String, ArrStringValue>>> it = group.entrySet().iterator();
	    while(it.hasNext()) {
	        Entry<String, HashMap<String, ArrStringValue>> e = it.next();
	        keys[b++] = e.getKey();
	    }
	    return keys;
	}

	public String[] names(String key) {
	    int b = 0, size = group.get(key).size();
	    if(size <= 0) return new String[0];
        String[] names = new String[size];
        Iterator<Entry<String, ArrStringValue>> it = group.get(key).entrySet().iterator();
        while(it.hasNext()) {
            Entry<String, ArrStringValue> e = it.next();
            names[b++] = e.getKey();
        }
        return names;
	}

	/**
	 * 读取配置
	 * @param groupkey
	 * @param childkey
	 * @param defaultvalue 默认值
	 * @return
	 */
	public String getStringValue(String groupkey, String childkey, String defaultvalue){
	    ArrStringValue val = getArrStringValue(groupkey, childkey, defaultvalue);
		return val.toString();
	}

	public ArrStringValue getArrStringValue(String groupkey, String childkey, String defaultvalue){
	    HashMap<String, ArrStringValue>  child = group.get(groupkey);
        if(child == null) return new ArrStringValue().put(defaultvalue);
        ArrStringValue val = child.get(childkey);
        if(val == null){
            ArrStringValue vals = new ArrStringValue();
            if(!Utils.CheckNull(defaultvalue)) {
                vals.put(defaultvalue);
            }
            return vals;
        }
        return val;
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

	public HashMap<String, String> getGroup(String key){
		HashMap<String, String> res = new HashMap<String, String>();
        Iterator<Entry<String, ArrStringValue>> it = group.get(key).entrySet().iterator();
        while(it.hasNext()) {
            Entry<String, ArrStringValue> e = it.next();
            res.put(e.getKey(), e.getValue().toString());
        }
		return res;
	}

	/**
	 * 设置值 没有则创建 此方法多次put会有多个key value
	 * @param groupkey
	 * @param childkey
	 * @param value
	 * @return
	 */
	public synchronized void addValue(String groupkey, String childkey, String value){
	    //read(lastReadCharset);
		HashMap<String, ArrStringValue>  child = group.get(groupkey);
		if(child == null){
			child = new HashMap<String, ArrStringValue>();
			child.put(childkey, new ArrStringValue().put(value));
			group.put(groupkey, child);
			return ;
		}
		ArrStringValue val = child.get(childkey);
		if(val == null){
		    child.put(childkey, new ArrStringValue().put(value));
		}else{
		    val.put(value);
		}
	}

	/**
	 * 此方法不会增加多个key
	 * @param groupkey
	 * @param childkey
	 * @param value
	 */
	public synchronized void setValue(String groupkey, String childkey, String value){
        //read(lastReadCharset);
	    HashMap<String, ArrStringValue>  child = group.get(groupkey);
        if(child == null){
            child = new HashMap<String, ArrStringValue>();
            child.put(childkey, new ArrStringValue().put(value));
            group.put(groupkey, child);
            return ;
        }
        ArrStringValue val = child.get(childkey);
        if(val == null){
            child.put(childkey, new ArrStringValue().put(value));
        }else{
            val.set(value);
        }
	}

	public synchronized void save(){
		if(file == null) {
		    return;
		}
		FileWriter fout = null;
		try {

	        fout = new FileWriter(file);

			fout.write(build().toString());
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

	public StringBuilder build(){
	    StringBuilder sb = new StringBuilder();
	    Iterator<Entry<String, HashMap<String, ArrStringValue>>> iter = group.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, HashMap<String, ArrStringValue>> entry = iter.next();
            String key = (String) entry.getKey();
            HashMap<String, ArrStringValue> val = entry.getValue();
            sb.append("[");
            sb.append(key);
            sb.append("]\n");
            Iterator<Entry<String, ArrStringValue>> vs = val.entrySet().iterator();
            while(vs.hasNext()){
                Entry<String, ArrStringValue> c_entry =  vs.next();
                String k = c_entry.getKey();
                ArrStringValue v = c_entry.getValue();
                for(String tv : v.getVal()){
                    sb.append(k);
                    sb.append("=");
                    sb.append(tv);
                    sb.append("\n");
                }
            }
        }
        return sb;
	}

	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
        Iterator<Entry<String, HashMap<String, ArrStringValue>>> iter = group.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, HashMap<String, ArrStringValue>> entry = iter.next();
            String key = (String) entry.getKey();
            HashMap<String, ArrStringValue> val = entry.getValue();
            sb.append("  [");
            sb.append(key);
            sb.append("]\n");
            Iterator<Entry<String, ArrStringValue>> vs = val.entrySet().iterator();
            while(vs.hasNext()){
                Entry<String, ArrStringValue> c_entry =  vs.next();
                String k = c_entry.getKey();
                ArrStringValue v = c_entry.getValue();
                for(String tv : v.getVal()){
                    sb.append("    ");
                    sb.append(k);
                    sb.append("=");
                    sb.append(tv);
                    sb.append("\n");
                }

            }

        }
		return "\nConfig [\n" + sb.toString() + "\n]";
	}

}
