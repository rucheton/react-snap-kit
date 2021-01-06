package com.mduthey.snapchat;

import android.app.Activity;
import android.net.Uri;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import androidx.annotation.Nullable;

import com.snapchat.kit.sdk.SnapLogin;
import com.snapchat.kit.sdk.core.controller.LoginStateController;
import com.snapchat.kit.sdk.login.networking.FetchUserDataCallback;
import com.snapchat.kit.sdk.login.models.MeData;
import com.snapchat.kit.sdk.login.models.UserDataResponse;

import com.snapchat.kit.sdk.SnapCreative;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitApi;
import com.snapchat.kit.sdk.creative.exceptions.SnapMediaSizeException;
import com.snapchat.kit.sdk.creative.exceptions.SnapVideoLengthException;
import com.snapchat.kit.sdk.creative.exceptions.SnapStickerSizeException;
import com.snapchat.kit.sdk.creative.media.SnapMediaFactory;
import com.snapchat.kit.sdk.creative.media.SnapPhotoFile;
import com.snapchat.kit.sdk.creative.media.SnapVideoFile;
import com.snapchat.kit.sdk.creative.media.SnapSticker;
import com.snapchat.kit.sdk.creative.models.SnapContent;
import com.snapchat.kit.sdk.creative.models.SnapLiveCameraContent;
import com.snapchat.kit.sdk.creative.models.SnapPhotoContent;
import com.snapchat.kit.sdk.creative.models.SnapVideoContent;

import java.io.File;

public class SnapchatLoginModule extends ReactContextBaseJavaModule {

    private static final String snapchatScheme = "com.snapchat.android";

    private final ReactApplicationContext reactContext;
    private final LoginStateController.OnLoginStateChangedListener mLoginStateChangedListener =
            new LoginStateController.OnLoginStateChangedListener() {
                @Override
                public void onLoginSucceeded() {
                    sendEvent("LoginSucceeded", null);
                }

                @Override
                public void onLoginFailed() {
                    sendEvent("LoginFailed", null);
                }

                @Override
                public void onLogout() {
                    sendEvent("LogOut", null);
                }
            };

    public SnapchatLoginModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    public void sendEvent(String eventName, @Nullable WritableMap params) {
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @Override
    public String getName() {
        return "SnapchatLogin";
    }

    @ReactMethod
    public void login(final Promise promise) {
        try {
            SnapLogin.getLoginStateController(getReactApplicationContext()).addOnLoginStateChangedListener(this.mLoginStateChangedListener);
            SnapLogin.getAuthTokenManager(getReactApplicationContext()).startTokenGrant();
            promise.resolve("{\"result\": true}");
        } catch (Exception e) {
            promise.resolve("{\"result\": false, \"error\": "+ e.toString() +"}");
        }
    }

    @ReactMethod
    public void logout(final Promise promise) {
        try {
            SnapLogin.getAuthTokenManager(getReactApplicationContext()).clearToken();
            promise.resolve("{\"result\": true}");
        } catch (Exception e) {
            promise.resolve("{\"result\": false, \"error\": "+ e.toString() +"}");
        }
    }

    @ReactMethod
    public void isUserLoggedIn(Promise promise) {
        boolean isTrue = SnapLogin.isUserLoggedIn(getReactApplicationContext());
        promise.resolve("{\"result\": " + isTrue + "}");
    }

    @ReactMethod
    public void fetchUserData(final Promise promise) {
        String query = "{me{bitmoji{avatar},displayName,externalId}}";
        if(SnapLogin.isUserLoggedIn(getReactApplicationContext())){
            SnapLogin.fetchUserData(getReactApplicationContext(), query, null, new FetchUserDataCallback() {
                @Override
                public void onSuccess(@Nullable UserDataResponse userDataResponse) {
                    if (userDataResponse == null || userDataResponse.getData() == null) {
                        promise.resolve(null);
                        return;
                    }

                    MeData meData = userDataResponse.getData().getMe();
                    if (meData == null) {
                        promise.resolve(null);
                        return;
                    }
                    String output = "{"
                            + "\"displayName\": \"" + meData.getDisplayName() + "\""
                            + ", \"externalId\": \"" + meData.getExternalId() + "\""
                            + ", \"avatar\": \"" + meData.getBitmojiData().getAvatar() + "\""
                            + ", \"accessToken\": \""+ SnapLogin.getAuthTokenManager(getReactApplicationContext()).getAccessToken() + "\""
                            + "}";
                    promise.resolve(output);
                }

                @Override
                public void onFailure(boolean b, int i) {
                    String I = Integer.toString(i);

                    promise.reject(I);
                }
            });
        } else {
            promise.resolve(null);
        }
    }

    @ReactMethod
    public void getAccessToken(final Promise promise) {
        try {
            String accessToken = SnapLogin.getAuthTokenManager(getReactApplicationContext()).getAccessToken();
            promise.resolve("{\"accessToken\": \"" + accessToken + "\"}");
        } catch (Exception e) {
            promise.resolve("{\"accessToken\": \"null\", \"error\": \"" + e.toString() + "\"}");
        }
    }

    @ReactMethod
    public void sharePhotoResolved(
                @Nullable String photoUrl,
                @Nullable String stickerUrl,
                @Nullable Float stickerPosX,
                @Nullable Float stickerPosY,
                @Nullable String attachmentUrl,
                @Nullable String caption,
                Promise promise
            ) {

//             Dynamic photo = resolvedPhoto.isNull() ? resolvedPhoto.asString() : photoUrl;
//             Dynamic sticker = stickerResolved.isNull() ? stickerResolved.asString() : stickerUrl;

            shareWithPhoto(photoUrl, null, stickerUrl, stickerPosX, stickerPosY, attachmentUrl, caption, promise);
    }
    @ReactMethod
    public void shareVideoAtUrl(
                @Nullable String videoUrl,
                @Nullable String stickerUrl,
                @Nullable Float stickerPosX,
                @Nullable Float stickerPosY,
                @Nullable String attachmentUrl,
                @Nullable String caption,
                Promise promise
            ) {
//             Dynamic sticker = stickerResolved.isNull() ? stickerResolved.asString() : stickerUrl;

            shareWithPhoto(null, videoUrl, stickerUrl, stickerPosX, stickerPosY, attachmentUrl, caption, promise);
    }

    private void canOpenUrl(String packageScheme, Promise promise){
        try{
          Activity activity = getCurrentActivity();
          activity.getPackageManager().getPackageInfo(packageScheme, PackageManager.GET_ACTIVITIES);
          promise.resolve(true);
        } catch (PackageManager.NameNotFoundException e) {
          promise.resolve(false);
        } catch (Exception e) {
          promise.resolve(new JSApplicationIllegalArgumentException(
                  "Could not check if URL '" + packageScheme + "' can be opened: " + e.getMessage()));
        }
      }

    private void shareWithPhoto(
          @Nullable String photoImageOrUrl,
          @Nullable String videoUrl,
          @Nullable String stickerImageOrUrl,
          @Nullable Float stickerPosX,
          @Nullable Float stickerPosY,
          @Nullable String attachmentUrl,
          @Nullable String caption,
          Promise promise
      ) {
        try {
          Activity activity = getCurrentActivity();
          SnapMediaFactory snapMediaFactory = SnapCreative.getMediaFactory(activity);
          SnapContent snapContent;
          SnapCreativeKitApi snapCreativeKitApi = SnapCreative.getApi(activity);


          if (videoUrl != null) {
              SnapVideoFile snVideoFile = null;
              try {
                final File videoFile = new File(videoUrl);
                if (videoFile.exists()) {
                    snVideoFile = snapMediaFactory.getSnapVideoFromFile(videoFile);
                } else {
                    promise.resolve("Error: Video file not found");
                }
              } catch (SnapMediaSizeException|SnapVideoLengthException e) {
                 promise.resolve(e.toString());
                 return;
              }
              snapContent = new SnapVideoContent(snVideoFile);

          } else if (photoImageOrUrl != null) {
              SnapPhotoFile photoFile = null;
              try {
                 final File imageFile = new File(photoImageOrUrl);
                 if (imageFile.exists()) {
                    photoFile = snapMediaFactory.getSnapPhotoFromFile(imageFile);
                 } else {
                    promise.resolve("Error: Image file not found");
                 }
              } catch (SnapMediaSizeException e) {
                 promise.resolve(e.toString());
                 return;
              }
              snapContent = new SnapPhotoContent(photoFile);
          } else {
              snapContent = new SnapLiveCameraContent();
          }

          if (stickerImageOrUrl != null) {
              SnapSticker snapSticker = null;


              try {
                    final File stickerFile = new File(stickerImageOrUrl);
                    if (stickerFile.exists()) {
                        snapSticker = snapMediaFactory.getSnapStickerFromFile(stickerFile);
                    } else {
                        promise.resolve("Error: Sticker file not found");
                    }
//                   if (stickerPosX != null) {
//                     snapSticker.setPosX(stickerPosX);
//                   }
//                   if (stickerPosY != null) {
//                     snapSticker.setPosY(stickerPosY);
//                   }

                  snapContent.setSnapSticker(snapSticker);
              } catch (SnapStickerSizeException e) {
                  promise.resolve(e.toString());
                  return;
              }
          }

          if (caption != null) {
            snapContent.setCaptionText(caption);
          }
          if (attachmentUrl != null) {
            snapContent.setAttachmentUrl(attachmentUrl);
          }

          snapCreativeKitApi.send(snapContent);

          promise.resolve("success");
        } catch (Exception e){
          promise.resolve("UNERROR: "+e.toString());
        }
      }

    @ReactMethod
    public void isSnapchatAvailable(final Promise promise) {
      canOpenUrl(snapchatScheme, promise);
    }

}
