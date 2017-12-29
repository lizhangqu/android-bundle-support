### android bundle support

支持ap_, ap, aar, jar, so, awb等zip文件使用apk analyzer打开，这类文件会添加android的图标，不是zip文件无任何提示，否则使用apk analyzer打开


### 支持的文件列表
 
 - android 编译产生的资源ap_文件
 - android 编译产生的aar文件
 - jar文件
 - so文件，注意只有在so为zip包的时候才会被打开，否则忽略，场景：android插件化
 - atlas的ap文件
 - atlas的awb文件
 
