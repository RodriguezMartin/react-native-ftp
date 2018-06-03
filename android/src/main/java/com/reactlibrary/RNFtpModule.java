
package com.reactlibrary;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.Util;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import javax.annotation.Nullable;
import android.text.format.Formatter;


public class RNFtpModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private static String ip_address;
  private static int port;
  private static FTPClient client;
  private static String outputDirectoryPath;

  public RNFtpModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    client = new FTPClient();
    client.setConnectTimeout(4000);
    client.setBufferSize(1024000);
  }

  @ReactMethod @Nullable
  public void setup(String ip_address, int port, @Nullable String outputDirectoryPath){
    this.ip_address = ip_address;
    this.port = port;
    if(outputDirectoryPath == null){
      this.outputDirectoryPath = reactContext.getFilesDir().getAbsolutePath();
    }
    else{
      this.outputDirectoryPath = Objects.equals(outputDirectoryPath.substring(outputDirectoryPath.length() - 1),"/")
        ? outputDirectoryPath.substring(0, outputDirectoryPath.length() - 1)
        : outputDirectoryPath;
      (new File(outputDirectoryPath)).mkdirs();
    }
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
          response.putBoolean("noop",client.sendNoOp());
          promise.resolve(response);
        } catch (Exception e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }

  void sortByName(ArrayList list){
    Collections.sort(list, new Comparator<FTPFile>() {
      @Override
      public int compare(FTPFile file1, FTPFile file2){
        return file1.getName().compareTo(file2.getName());
      }
    });
  }

  @ReactMethod
  public void list(final String path, final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        FTPFile[] all = new FTPFile[0];
        ArrayList<FTPFile> dirs = new ArrayList<FTPFile>();
        ArrayList<FTPFile> files = new ArrayList<FTPFile>();
        try {
          all = client.listFiles(path);
          for (FTPFile file : all) {
            if(file.isDirectory()){
              dirs.add(file);
            }
            else{
              files.add(file);
            }
          }
          sortByName(dirs);
          sortByName(files);
          dirs.addAll(files);
          WritableArray arrfiles = new WritableNativeArray();
          int key = 0;
          for (FTPFile file : dirs) {
            key = 1 + key;
            WritableMap tmp = new WritableNativeMap();
            tmp.putString("key",String.valueOf(key));
            tmp.putString("name",file.getName());
            tmp.putString("readableSize",Formatter.formatShortFileSize(reactContext, file.getSize()));
            tmp.putString("size",String.valueOf(file.getSize()));
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

  private void sendEvent(String eventName, int progress) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, progress);
  }

  @ReactMethod @Nullable
  public void downloadFile(final String listenerName, final ReadableMap remoteFile, @Nullable final String localDestinationDir, final Promise promise){
    new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          client.setFileType(FTP.BINARY_FILE_TYPE);
          
          String destDir;
          if(localDestinationDir == null){
            destDir = outputDirectoryPath;
          }
          else{
            destDir = Objects.equals(localDestinationDir.substring(0,1),"/")
              ? localDestinationDir
              : outputDirectoryPath +"/"+ localDestinationDir;
            if(Objects.equals(destDir.substring(destDir.length() - 1),"/")){
              destDir = destDir.substring(0, destDir.length() - 1);
            }
            (new File(destDir)).mkdirs();
          } 

          InputStream input = new BufferedInputStream(client.retrieveFileStream(remoteFile.getString("filePath")));
          OutputStream output = new FileOutputStream(destDir+"/"+remoteFile.getString("name"));

          CopyStreamAdapter adapter = new CopyStreamAdapter() {
            int current = 0;
            public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
              int temp = (int) Math.ceil(totalBytesTransferred  * 100 / streamSize);
              if(temp != current){
                current = temp;
                sendEvent(listenerName, current);
              }
            }
          };

          Util.copyStream(
            input,
            output,
            client.getBufferSize(),
            Long.parseLong(remoteFile.getString("size"), 10),
            adapter
          );
          input.close();
          output.close();
          boolean ok = client.completePendingCommand();
          if(ok){
            promise.resolve(true);
          }
          else{
            promise.reject("FAILED","not downloaded successfully.");
          }
        } catch (Exception e) {
          promise.reject("ERROR",e.getMessage());
        }
      }
    }).start();
  }

  @ReactMethod
  public void abort(final Promise promise){
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          client.abort();
          promise.resolve(true);
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
