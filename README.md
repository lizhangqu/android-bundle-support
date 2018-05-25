### android bundle support

增强型apk analyzer，支持ap_, ap, aar, jar, so, awb等zip文件使用apk analyzer打开，这类文件会添加android的图标，不是zip文件无任何提示，否则使用apk analyzer打开，其中jar文件不修改图标。


### 支持的文件列表
 
 - android 编译产生的资源ap_文件
 - android 编译产生的aar文件
 - android app bundle编译产生的aab文件
 - jar文件
 - war文件
 - so文件，注意只有在so为zip包的时候才会被打开，否则忽略，场景：android插件化
 - atlas的ap文件
 - atlas的awb文件
 
### 插件仓库安装

 - Android Studio -> Preferences -> Plugins -> Browse repositories
 - Search 'Android Bundle Support' and install it
 - restart Android Studio
 
 
### 本地安装

 - 下载 [Android-Bundle-Support-1.0.2.zip](https://raw.githubusercontent.com/lizhangqu/android-bundle-support/master/release/Android-Bundle-Support-1.0.2.zip)
 - Android Studio -> Preferences -> Plugins -> Install plugin from disk, 选择文件
 - 重启Android Studio

### 支持Android Studio版本
 
 - Android Studio 3.0.0
 - Android Studio 3.0.1
 - Android Studio 3.1.0
 - Android Studio 3.2.0
 