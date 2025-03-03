package com.mduthey.snapchat;

import com.facebook.imagepipeline.core.FileCacheFactory;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import androidx.annotation.Nullable;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;

import com.snapchat.kit.sdk.SnapCreative;
import com.snapchat.kit.sdk.SnapLogin;
import com.snapchat.kit.sdk.core.controller.LoginStateController;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitApi;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitCompletionCallback;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitSendError;
import com.snapchat.kit.sdk.creative.exceptions.SnapMediaSizeException;
import com.snapchat.kit.sdk.creative.exceptions.SnapStickerSizeException;
import com.snapchat.kit.sdk.creative.exceptions.SnapVideoLengthException;
import com.snapchat.kit.sdk.creative.media.SnapSticker;
import com.snapchat.kit.sdk.creative.models.SnapLiveCameraContent;
import com.snapchat.kit.sdk.creative.models.SnapPhotoContent;
import com.snapchat.kit.sdk.creative.models.SnapVideoContent;
import com.snapchat.kit.sdk.login.networking.FetchUserDataCallback;
import com.snapchat.kit.sdk.login.models.MeData;
import com.snapchat.kit.sdk.login.models.UserDataResponse;
import com.snapchat.kit.sdk.creative.models.SnapContent;
import com.snapchat.kit.sdk.creative.media.SnapMediaFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


class IncorrectFileNameException extends Exception {
    public IncorrectFileNameException(String errorMessage) {
        super(errorMessage);
    }
}

public class SnapchatKitModule extends ReactContextBaseJavaModule {
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

    private final SnapMediaFactory snapMediaFactory;
    private final SnapCreativeKitApi snapCreativeKitApi;


    public SnapchatKitModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.snapMediaFactory = SnapCreative.getMediaFactory(this.reactContext);
        this.snapCreativeKitApi = SnapCreative.getApi(this.reactContext);
    }

    public void sendEvent(String eventName, @Nullable WritableMap params) {
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    @Override
    public String getName() {
        return "SnapchatKit";
    }

    @ReactMethod
    public void login() {
        SnapLogin.getLoginStateController(getReactApplicationContext()).addOnLoginStateChangedListener(this.mLoginStateChangedListener);
        SnapLogin.getAuthTokenManager(getReactApplicationContext()).startTokenGrant();
    }

    @ReactMethod
    public void logout() {
        SnapLogin.getAuthTokenManager(getReactApplicationContext()).clearToken();
    }

    @ReactMethod
    public void isUserLoggedIn(Promise promise) {
        boolean isTrue = SnapLogin.isUserLoggedIn(getReactApplicationContext());
        promise.resolve(isTrue);
    }

    @ReactMethod
    public void fetchUserData(final Promise promise) {
        String query = "{me{bitmoji{avatar},displayName,externalId}}";
        if(SnapLogin.isUserLoggedIn(getReactApplicationContext())){
            SnapLogin.fetchUserData(getReactApplicationContext(), query, null, new FetchUserDataCallback() {
                @Override
                public void onSuccess(@Nullable UserDataResponse userDataResponse) {
                    if (userDataResponse == null || userDataResponse.getData() == null) {
                        promise.reject("no data");
                        return;
                    }

                    MeData meData = userDataResponse.getData().getMe();
                    if (meData == null) {
                        promise.reject("no data");
                        return;
                    }

                    WritableMap map = Arguments.createMap();

                    map.putString("displayName", meData.getDisplayName());
                    map.putString("externalId", meData.getExternalId());
                    map.putString("avatar", meData.getBitmojiData().getAvatar());
                    map.putString("accessToken", SnapLogin.getAuthTokenManager(getReactApplicationContext()).getAccessToken());

                    promise.resolve(map);
                }

                @Override
                public void onFailure(boolean b, int i) {
                    String I = Integer.toString(i);

                    promise.reject(I);
                }
            });
        } else {
            promise.reject("unauthenticated");
        }
    }

    @ReactMethod
    public void getAccessToken(final Promise promise) {
        try {
            String accessToken = SnapLogin.getAuthTokenManager(getReactApplicationContext()).getAccessToken();
            promise.resolve(accessToken);
        } catch (Exception e) {
            promise.reject("unauthenticated");
        }
    }

    @ReactMethod
    public void sharePhotoResolved(ReadableMap photoResolved, String photoUrl, ReadableMap stickerResolved, String stickerUrl, Float stickerPosX, Float stickerPosY, String attachmentUrl, String caption, final Promise promise) {
        this.shareWithPhoto(photoResolved, photoUrl, null, stickerResolved, stickerUrl, stickerPosX, stickerPosY, attachmentUrl, caption, promise);
    }

    @ReactMethod
    public void shareVideoAtUrl(String videoUrl, ReadableMap stickerResolved, String stickerUrl, Float stickerPosX, Float stickerPosY, String attachmentUrl, String caption, final Promise promise) {
        this.shareWithPhoto(null, null, videoUrl, stickerResolved, stickerUrl, stickerPosX, stickerPosY, attachmentUrl, caption, promise);
    }

    private String md5Base64(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(input.getBytes());
        byte[] digest = md.digest();

        return  Base64.encodeToString(digest, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }

    private File getFileFromUri(String uri) throws IncorrectFileNameException,IOException, NoSuchAlgorithmException {
        if (TextUtils.isEmpty(uri)) {
            throw new IncorrectFileNameException("Incorrect URI:" + uri);
        }
        if (uri.startsWith("file://")) {
            return new File(uri.substring(7));
        }
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            final URL url = new URL(uri);
            File file = new File(this.reactContext.getCacheDir(), md5Base64(url.toString()));
            InputStream inputStream = url.openStream();
            try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                int read;
                byte[] bytes = new byte[8192];
                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
                outputStream.flush();
            } finally {
                inputStream.close();
            }
            return file;
        }
        if (uri.indexOf("://") == -1 && uri.indexOf("/") == -1) {
            String[] deftypes = {"drawable", "raw"};
            for (String defType: deftypes) {
                int resourceId = this.reactContext.getResources().getIdentifier(uri, defType, this.reactContext.getPackageName());
                if (resourceId == 0) {
                    continue;
                }
                TypedValue value = new TypedValue();
                File file = new File(this.reactContext.getCacheDir(), md5Base64(uri));
                InputStream inputStream = this.reactContext.getResources().openRawResource(resourceId);
                try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                    int read;
                    byte[] bytes = new byte[8192];
                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }
                    outputStream.flush();
                } finally {
                    inputStream.close();
                }
                return file;
            }
        }
        throw new IncorrectFileNameException("Incorrect URI:" + uri);
    }

    private void shareWithPhoto(ReadableMap photoResolved, String photoUrl, String videoUrl, ReadableMap stickerResolved, String stickerUrl, Float stickerPosX, Float stickerPosY, String attachmentUrl, String caption, final Promise promise) {
        try {
            try {
                final SnapContent content;

                if (!TextUtils.isEmpty(videoUrl)) {
                    content = new SnapVideoContent(snapMediaFactory.getSnapVideoFromFile(getFileFromUri(videoUrl)));
                } else if (!TextUtils.isEmpty(photoUrl)) {
                    content = new SnapPhotoContent(snapMediaFactory.getSnapPhotoFromFile(getFileFromUri(photoUrl)));
                } else if (photoResolved != null) {
                    content = new SnapPhotoContent(snapMediaFactory.getSnapPhotoFromFile(getFileFromUri(photoResolved.getString("uri"))));
                } else { //TODO resolvedPhoto
                    content = new SnapLiveCameraContent();
                }
                if (!TextUtils.isEmpty(caption)) {
                    content.setCaptionText(caption);
                }
                if (!TextUtils.isEmpty(attachmentUrl)) {
                    content.setAttachmentUrl(attachmentUrl);
                }
                if (!TextUtils.isEmpty(stickerUrl) || stickerResolved != null) { //TODO stickerResolved
                    final SnapSticker snapSticker;
                    if (stickerResolved != null) {
                        snapSticker = snapMediaFactory.getSnapStickerFromFile(getFileFromUri(stickerResolved.getString("uri")));
                        if (stickerResolved.hasKey("width") && stickerResolved.hasKey("height")) {
                            snapSticker.setWidthDp((float) stickerResolved.getDouble("width"));
                            snapSticker.setHeightDp((float) stickerResolved.getDouble("height"));
                        } else {
                            snapSticker.setWidthDp(300);
                            snapSticker.setHeightDp(300);
                        }
                    } else {
                        snapSticker = snapMediaFactory.getSnapStickerFromFile(getFileFromUri(stickerUrl));
                        snapSticker.setWidthDp(300);
                        snapSticker.setHeightDp(300);
                    }
                    snapSticker.setPosX(stickerPosX);
                    snapSticker.setPosY(stickerPosY);
                    content.setSnapSticker(snapSticker);
                }
                snapCreativeKitApi.sendWithCompletionHandler(content, new SnapCreativeKitCompletionCallback() {
                    @Override
                    public void onSendSuccess() {
                        promise.resolve(true);
                    }

                    @Override
                    public void onSendFailed(SnapCreativeKitSendError snapCreativeKitSendError) {
                        promise.reject(snapCreativeKitSendError.toString() + " " + snapCreativeKitSendError.name());
                    }
                });
            } catch (SnapMediaSizeException | SnapVideoLengthException e) {
                promise.reject(e.getLocalizedMessage());
            } catch (Exception e) {
                promise.reject(e.getLocalizedMessage());
            }
        } catch (Exception e) {
            promise.reject("Unknown JSONexception");
        }
    }
}