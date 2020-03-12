# EasyStorer
@[TOC](目录)
还在使用Android SDK中的笨重的SharedPreferences嘛？
或者不断重复造的SQLite数据库？
不如试试这个EasyStorer?
像使用Map<String,Object>一样存储数据对象到本地。
https://blog.csdn.net/best335/article/details/104772571
# EasyStorer的使用
EasyStorer静态公有函数，怎么样，看了这个类结构，大概不用我说该怎么用了叭？
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200310165055702.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70)
修改app的build.gradle文件
```bash
dependencies {
	...
	//1.0版本不要在多线程中操作同一个数据库，正在修复异常
	api "com.github.isong0623:EasyStorer:1.0-support"
	...
}
```
在Application初始化(没有则新建Application类)

```java
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EasyStorer.init(this);//在这里初始化
    }
}
```
如果新建App则需在AndroidManifest.xml里指定Application
```java
    <application
    	...
    	android:name=".App"
    	...>
```
### 正式使用
#### 创建一个自定义类

```java
class Obj implements Serializable{//只需要实现这个接口
    int a = 100;
    int b = 1000;
    //ObjA o; 如果成员变量为非Java定义类型 则它自己及它的成员也必须实现Serializable接口
    Obj(){}
    Obj(int a, int b){
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString() {
        return String.format("val1:%d val2:%d\n",a,b);
    }
}
```

##### 简单存储示例
```java
EasyStorer.clear();//清除默认库里的所有数据

EasyStorer.put("Obj", new Obj(123,132));
EasyStorer.put("String","String");
EasyStorer.put("byte",(byte)10);
EasyStorer.put("Byte",new Byte((byte) 15));
EasyStorer.put("short",(short)10);
EasyStorer.put("Short",new Short((short)15));
EasyStorer.put("boolean",true);
EasyStorer.put("Boolean",new Boolean(true));
EasyStorer.put("int",10);
EasyStorer.put("Integer",15);
EasyStorer.put("long",10L);
EasyStorer.put("Long",new Long(15L));
EasyStorer.put("float",10f);
EasyStorer.put("Float",15f);
EasyStorer.put("double",10d);
EasyStorer.put("Double",new Double(15d));
EasyStorer.put("List",new ArrayList<>(Arrays.asList(new String[]{"123","456"})));
EasyStorer.put("Set",new HashSet<>(Arrays.asList(new String[]{"123","456"})));
EasyStorer.put("Map",new HashMap<>(new HashMap<String,String>(){{put("abc","edf");}}));
```
##### 简单获取示例
```java
Log.e("TestEasyStorer",EasyStorer.get("Obj",new Obj())+"");
Log.e("TestEasyStorer",EasyStorer.get("String","null"));
Log.e("TestEasyStorer",EasyStorer.get("byte",(byte)0)+"");
Log.e("TestEasyStorer",EasyStorer.get("Byte",new Byte((byte)0))+"");
Log.e("TestEasyStorer",EasyStorer.get("short",(short)0)+"");
Log.e("TestEasyStorer",EasyStorer.get("Short",new Short((short)0))+"");
Log.e("TestEasyStorer",EasyStorer.get("boolean",false)+"");
Log.e("TestEasyStorer",EasyStorer.get("Boolean",new Boolean(false))+"");
Log.e("TestEasyStorer",EasyStorer.get("int",0)+"");
Log.e("TestEasyStorer",EasyStorer.get("Integer",new Integer(0))+"");
Log.e("TestEasyStorer",EasyStorer.get("long",0L)+"");
Log.e("TestEasyStorer",EasyStorer.get("Long",new Long(0L))+"");
Log.e("TestEasyStorer",EasyStorer.get("float",0f)+"");
Log.e("TestEasyStorer",EasyStorer.get("Float",new Float(0f))+"");
Log.e("TestEasyStorer",EasyStorer.get("double",0d)+"");
Log.e("TestEasyStorer",EasyStorer.get("Double",new Double(0d))+"");
Log.e("TestEasyStorer",EasyStorer.get("List",new ArrayList<String>()).toArray()[0]+"");
Log.e("TestEasyStorer",EasyStorer.get("Set",new HashSet<>()).toArray()[0]+"");
Log.e("TestEasyStorer",EasyStorer.get("Map",new HashMap<>()).keySet().toArray()[0]+"");	
```
##### 示例输出

```bash
E/TestEasyStorer: val1:123 val2:132
E/TestEasyStorer: String
E/TestEasyStorer: 10
E/TestEasyStorer: 15
E/TestEasyStorer: 10
E/TestEasyStorer: 15
E/TestEasyStorer: true
E/TestEasyStorer: true
E/TestEasyStorer: 10
E/TestEasyStorer: 15
E/TestEasyStorer: 10
E/TestEasyStorer: 15
E/TestEasyStorer: 10.0
E/TestEasyStorer: 15.0
E/TestEasyStorer: 10.0
E/TestEasyStorer: 15.0
E/TestEasyStorer: 123
E/TestEasyStorer: 123
E/TestEasyStorer: abc
```
### 疑问
Q:为什么有两个put和两个get?
A:我们所有的数据引用都是放在SQLite数据库中的，两个参数的函数指定了默认数据库（/data/data/com.example.package/databases/_Easy_Storer_.db_），与此同时，还有一个以该数据库命名的文件夹（/data/data/com.example.package/databases/_Easy_Storer_/）用于存放串行化的数据，以数字命名。我们可以对不同群体使用不同的数据库名称进行操作，例如我们使用“user_info”存储用户信息，用"app_info"存储应用信息等更方便地管理应用中的角色。

### API详解
###### 初始化

```java
	//初始化context,在这里仅用来获取内部数据库存储路径
	public static void init(Context context);
```
###### 取数据操作

```java
	/**
     * 取数据操作
     * @param tag 标签
     * @param defaultValue 取不到时返回的值
     * @param <T>
     * @return 在默认库（_Easy_Storer_）中取出标签为tag且类型为T的对象
     */
	public static <T> T get(String tag,T defaultValue);

 	/**
     * 取数据操作
     * @param tag 标签
     * @param defaultValue 取不到时返回的值
     * @param databaseName 要操作的数据库名称
     * @param <T>
     * @return 在指定库（databaseName ）中取出标签为tag且类型为T的对象
     */
    public static <T> T get(String tag,T defaultValue,String databaseName);
```
###### 存数据操作
```java
	/**
     * 在默认库（_Easy_Storer_）中存放对象obj置标签为tag
     * @param tag 存放标签
     * @param obj 待存放的对象
     * @return 返回是否存放成功
     */
    public static boolean put(String tag,Object obj);

 	/**
     * 在指定库（databaseName）中存放对象obj置标签为tag
     * @param tag 存放标签
     * @param obj 待存放的对象
     * @param databaseName 要存放数据的数据库名
     * @return 返回是否存放成功
     */
    public static boolean put(String tag,Object obj,String databaseName);
```
###### 删除数据操作

```java
	/**
     * 删除默认库（_Easy_Storer_）标签为tag的所有对象数据
     * @param tag 要删除的标签
     * @return 删除是否成功
     */
    public static boolean remove(String tag)

	/**
     * 删除指定数据库（databaseName）中标签为tag的所有数据
     * @param tag
     * @param databaseName
     * @return 删除是否成功
     */
    public static boolean remove(String tag,String databaseName)

	/**
     * 删除默认库（_Easy_Storer_）中标签为tag，类型为clazz的数据
     * @param tag
     * @param clazz
     * @return 删除是否成功
     */
    public static boolean remove(String tag,Class clazz)

	/**
     * 删除指定库（databaseName）中标签为tag，类型为clazz的数据
     * @param tag
     * @param clazz
     * @param databaseName
     * @return
     */
    public static boolean remove(String tag,Class clazz, String databaseName)
```

###### 清空数据操作
```java
	//删除默认数据库（_Easy_Storer_）的所有对象数据
	public static boolean clear();

	//删除指定数据库（databaseName）中的所有对象数据
	public static boolean clear(String databaseName)
```
# EasyStorer的优势
* 极低的学习成本。
* 快速存取的方法调用。
* 封装了Serializable的对象存取。
* 使用同步锁，保证线程安全。
* 一次存放处处可取（服务器（以后的版本）、客服端）。


# 使用EasyStorer的前提
* 需要在Application的onCreate()中初始化，否则调用EasyStorer时会**抛出运行时异常**，中断程序运行。
* 对**非Java SDK封装的类**及其成员变量及其父类相关的类成员及变量等等必须实现空接口Serializable，**类包名不得以java开头**。
	* 一般只要是自己写的类都需要实现Serializable空接口。
	* 如果只需要存取*基础数据类型*或*Java已封装的类型*，则无需实现。
	
# EasyStorer使用性能测试
所有测试增删查执行上文所述“**正式使用**”
## 同步测试（真机OPPO A57）
使用若干个线程操作同一个数据库在，其测试结果如下：
| 使用线程数 | 内存使用 | 异常情况 |
|--|--|--|
| 100 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312100653648.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70) | |
| 200 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312100357311.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70) | |
| 300 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312101128875.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70) | 无 |
| 500 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312100044971.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70) | 无 |
| 1000 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312102419457.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70) | 无 |


## 异步测试（真机OPPO A57）
通过对若干线程同时增删查，且每个线程操作单独的数据库，我们得到以下测试结果：
| 使用线程数 | 内存使用 | 异常情况 |
|--|--|--|
| 100 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312111949872.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70)| 无 |
| 200 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/2020031211171811.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70) | 无 |
| 300 |![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312111152280.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70) | 无 |
| 500 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312110234603.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70) | 无 |
| 1000 | ![在这里插入图片描述](https://img-blog.csdnimg.cn/20200312104838272.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2Jlc3QzMzU=,size_16,color_FFFFFF,t_70)  | RuntimeException: unable to open database file |
