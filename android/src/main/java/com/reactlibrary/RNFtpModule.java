
package com.reactlibrary;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RNFtpModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private static String ip_address;
  private static int port;
  private static FTPClient client;

  public RNFtpModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    client = new FTPClient();
    client.setConnectTimeout(4000);
  }

  @ReactMethod
  public void setup(String ip_address, int port){
    this.ip_address = ip_address;
    this.port = port;
  }

  @ReactMethod
  public void login(final String username, final String password, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.connect(RNFtpModule.this.ip_address,RNFtpModule.this.port);
          client.enterLocalPassiveMode();
          client.login(username, password);
          int status = client.getReplyCode();
          if (status == 230) {
            promise.resolve(true);
          }else{
            promise.reject(
              String.valueOf(status)
            );
          }
        } catch (Exception e) {
          promise.reject(
            String.valueOf(client.getReplyCode()),
            e.getMessage()
          );
        }
      }
    }).start();
  }

  @ReactMethod
  public void getStatus(final Promise promise) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          WritableMap response = new WritableNativeMap();
          response.putInt("code",client.getReplyCode());
          response.putString("text",client.getReplyString());
          promise.resolve(response);
        } catch (Exception e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }

  @ReactMethod
  public void list(final String path, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        FTPFile[] files = new FTPFile[0];
        try {
          files = client.listFiles(path);
          WritableArray arrfiles = new WritableNativeArray();
          int key = 0;
          for (FTPFile file : files) {
            key = 1 + key;
            WritableMap tmp = new WritableNativeMap();
            tmp.putString("key",String.valueOf(key));
            tmp.putString("name",file.getName());
            tmp.putString("size",String.valueOf(file.getSize()));
            tmp.putString("timestamp",String.valueOf(file.getTimestamp().getTimeInMillis()));
            tmp.putString("filePath",path + file.getName() + "/");
            tmp.putBoolean("isDir",file.isDirectory());
            arrfiles.pushMap(tmp);
          }
          promise.resolve(arrfiles);
        } catch (Exception e) {
          promise.reject("ERROR", e.getMessage());
        }
      }
    }).start();
  }

  @ReactMethod
  public void makedir(final String path, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.makeDirectory(path);
          promise.resolve(true);
        } catch (IOException e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }

  @ReactMethod
  public void removedir(final String path, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.removeDirectory(path);
          promise.resolve(true);
        } catch (IOException e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }
  @ReactMethod
  public void removeFile(final String path, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.deleteFile(path);
          promise.resolve(true);
        } catch (IOException e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }

  @ReactMethod
  public void changeDirectory(final String path, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.changeWorkingDirectory(path);
          promise.resolve(true);
        } catch (IOException e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }


  @ReactMethod
  public void logout(final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.logout();
          client.disconnect();
          promise.resolve(true);
        } catch (IOException e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }


  @ReactMethod
  public void uploadFile(final String path,final String remoteDestinationDir, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.setFileType(FTP.BINARY_FILE_TYPE);
          File firstLocalFile = new File(path);

          String firstRemoteFile = remoteDestinationDir+"/"+firstLocalFile.getName();
          InputStream inputStream = new FileInputStream(firstLocalFile);

          System.out.println("Start uploading first file");
          boolean done = client.storeFile(firstRemoteFile, inputStream);
          inputStream.close();
          if (done) {
            promise.resolve(true);
          }else{
            promise.reject("FAILED",firstLocalFile.getName()+" is not uploaded successfully.");
          }
        } catch (IOException e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }


  @ReactMethod
  public void downloadFile(final String remoteFile1,final String localDestinationDir, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.setFileType(FTP.BINARY_FILE_TYPE);
          File remoteFile = new File(remoteFile1);
          File downloadFile1 = new File(localDestinationDir+"/"+remoteFile.getName());
          OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1));
          boolean success = client.retrieveFile(remoteFile1, outputStream1);
          outputStream1.close();

          if (success) {
            promise.resolve(true);
          }else{
            promise.reject("FAILED",remoteFile.getName()+" is not downloaded successfully.");
          }
        } catch (IOException e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }


  @Override
  public String getName() {
    return "FTP";
  }
}
