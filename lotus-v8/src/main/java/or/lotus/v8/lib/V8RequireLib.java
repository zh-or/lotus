package or.lotus.v8.lib;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import or.lotus.v8.Message;
import or.lotus.v8.V8Context;
import or.lotus.v8.support.JavaLibBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;

public class V8RequireLib extends JavaLibBase {

    private V8Context base = null;
    private V8 runtime = null;

    @Override
    public void onInit(V8Context v8b) {
        base = v8b;
        runtime = v8b.getRuntimer();
        V8Object model = new V8Object(runtime);
        runtime.add("module", model);
        model.close();
        runtime.registerJavaMethod(this, "require", "require", new Class<?>[] {String.class});
    }

    /**
     * 会搜寻 LIB_PATH 配置项的所有目录
     * @param path
     * @return
     * @throws Exception
     */
    public V8Object require(String path) throws Exception {

        File libFile = new File(path);
        if(libFile == null) {
            throw new FileNotFoundException(path + " 未找到");
        }
        String requirejs = new String(Files.readAllBytes(libFile.toPath()), "utf-8");
        runtime.executeVoidScript(requirejs, libFile.getName(), 0);

        //从 model.export 读取
        V8Object model = runtime.getObject("module");
        V8Object export = model.getObject("export");
        model.addNull("export");
        model.close();

        return export;
    }

    @Override
    public void onQuit() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean MessageLoop(Message msg) {

        return false;
    }

}
