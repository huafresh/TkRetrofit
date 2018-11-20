# 前言
项目里的网络请求都是基于框架来做的，但是直接使用框架的api比较麻烦，一般都会再做一层封装。壳子里面的封装本人目前知道的有TkRequest、rxandmvplib。TkRequest暂且不表。今天我们来说一说rxandmvplib。
rxandmvplib这个封装是后面出来的，借鉴了了一些时髦的框架，比如Rxjava和Retrofit，使得网络请求支持了rx。其实针对retrofit，有个第三方库也是可以支持rx的，但是因为底层网络请求要走框架，所以rxandmvplib自定义了一些注解，然后仿照retrofit的注解解析方式进行解析，最后得到RequestBean然后执行请求。具体可以[参考这里](http://192.168.90.155/wanlh/NetWork)。
然而，本人在使用的过程感觉rxandmvplib还是存在一些不足，比如：
1、不支持修改请求方式（当然，框架也就支持了get和post）；
2、不支持添加静态header。比如，retrofit可以这样添加header
```
@Headers({
            Constant.PARAM_COMPANYID + ":THINKIVE",
            Constant.PARAM_SYSTEMID + ":LCSMALL"
    })
```
3、不支持上传等等。
总之差不多retrofit稍微复杂一点的功能都没有支持。其实也很好理解，因为rxandmvplib为了底层网络请求使用框架，采用了自定义注解的方式。如果要支持retrofit的所有功能，那不就相当于重写了retrofit框架了吗，，，，
那么，我们能不能直接利用retrofit做接口注解的解析，然后同时网络请求也换成框架呢？
答案是可以的，本文将给大家分享下实现思路。

# 思路
首先我们需要搞清楚，retrofit是如何发起网络请求的，然后看看能不能通过某种方式进行替换。
来看一个很简单的retrofit做请求的例子：
首先是接口：
```
@GET(value = "/login")
Call<ResponseBody> request(@Query("name") String name);
```
然后是发请求：

```
Retrofit retrofit = new Retrofit.Builder().build();
TestRequest api = retrofit.create(TestRequest.class);
Call<ResponseBody> call = api.request("name");
call.enqueue(new Callback<ResponseBody>() {
    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        
	}

	@Override
	public void onFailure(Call<ResponseBody> call, Throwable t) {
	
	}
});
```
重点是create出了接口实例后，我们调用接口方法后拿到了一个call对象，然后用这个call进行enqueue操作。感觉和okhttp的写法还是很像的，但是注意此call非彼call。
这个enqueue跟进去就会发现，它里面会创建一个okhttp的call，然后再执行okhttp的call的enqueue方法发起网络请求。大家想一下，发起网络请求肯定是要参数的，并且这个参数肯定在retrofit返回的call对象里。那么，假如我们可以把这个retrofit的call换成自定义的一个类（可以是继承 or 代理等），然后把call里面的各种请求参数转换成框架需要的RequestBean，再发起网络请求，就完美实现我们的需求了。

正好，retrofit支持自定义接口的返回值类型。可以继承CallAdapter.Factory实现，其中有个关键的抽象方法，叫adapter，如下：

```
@Override public <R> Object adapt(Call<R> call) {
     
}
```
它会传给我们一个原来默认返回的call，然后我们根据需要返回自定义的对象。retrofit解析得到的参数被封装成了一个Request对象，可以通过的call的如下方法获取：

```
/** The original HTTP request. */
  Request request();
```
那么至此，思路就很清晰了。我们可以在上述的adapt方法中返回一个自定义的对象，把Reqeust参数传进去，结合rx，在subscribeActual方法中使用框架发起网络请求，即可完美实现需求。

# 后记
基于上述思想，本人抛砖引玉，实现了TkRetrofit库。欢迎大家一起探讨。
实践的过程中发现了一个很蛋疼的问题，看看各位有没有解决思路：
retrofit一定要指定请求方式，不然会崩。这时候如果请求类型是socket的话，使用者会感觉莫名其妙。








