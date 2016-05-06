
#### 群号 347596815
#### github https://github.com/zh-or/lotus
###此工具包包含内容:

    1. Util    : 一些常用的方法
    2. socket  : tcp 长连接 服务端&客户端(已处理:断包/沾包问题)
    3. nio     : java nio 的服务端框架(目前就写了tcp的, udp 还没有写, kcp 是一个基于udp协议的东西, 原版是c/c++写的, 有空了再写个java版本的. 关于nio的客户端就放到最后再写咯.)
    4. map     : 目前就一个 intmap 是从Android源代码中copy出来的, 用来代替 HashMap<Integer, Object> 的一个东西
    5. log     : 格式化日志输出
    6. json    : json操作相关, copy from android source
    7. http    : 一个轻量级的http服务器
    8. format  : 字符串格式化相关
    9. config  : ini配置文件读写相关
    10. cluster : 轻量级集群消息通讯系统
