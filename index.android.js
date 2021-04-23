import { NativeModules, NativeEventEmitter } from 'react-native';

export const RNSnapchatKit = NativeModules.SnapchatKit;
export const RNSnapchatKitEmitter = new NativeEventEmitter(RNSnapchatKit);

export default class SnapchatKit {
  static addListener(eventType, listener, context) {
    return RNSnapchatKitEmitter.addListener(eventType, listener, context);
  }

  static login() {
    return new Promise((resolve, reject) => {
      const succeededListener = this.addListener('LoginSucceeded', (res) => {
        succeededListener.remove();
        failedListener.remove();
        this.getUserInfo().then(resolve).catch(reject); 
      });
      const failedListener = this.addListener('LoginFailed', (res) => {
        succeededListener.remove();
        failedListener.remove();
        resolve(false);
      });
      RNSnapchatKit.login();
    });
  }

  static async isLogged() {
    const result = await RNSnapchatKit.isUserLoggedIn();
    const resultJSON = JSON.parse(result);
    return !!resultJSON.result;
  }

  static async logout() {
    const result = await RNSnapchatKit.logout();
    const resultJSON = JSON.parse(result);
    return !!resultJSON.result;
  }

  static getUserInfo() {
    return new Promise((resolve, reject) => {
      RNSnapchatKit.fetchUserData()
        .then((tmp) => {
          const data = JSON.parse(tmp);
          if (data === null) {
            resolve(null);
          } else {
            resolve(data);
          }
        })
        .catch(e => reject(e));
    });
  }

  static async sharePhoto(photoImageSourceOrUrl, stickerImageSourceOrUrl, stickerPosX, stickerPosY, attachmentUrl, caption) {
    const resolveAssetSource = require('react-native/Libraries/Image/resolveAssetSource');
 
    const resolvedPhoto = resolveAssetSource(photoImageSourceOrUrl);
    const resolvedSticker = resolveAssetSource(stickerImageSourceOrUrl);
    const result = await RNSnapchatKit.sharePhotoResolved(
      resolvedPhoto, resolvedPhoto == null ? photoImageSourceOrUrl : null,
      resolvedSticker, resolvedSticker == null ? stickerImageSourceOrUrl : null,
      stickerPosX, stickerPosY,
      attachmentUrl,
      caption).catch(e => {
        reject(e)
      });
    const resultJSON = JSON.parse(result);
    if (!!resultJSON.error) {
      throw resultJSON.error
    }
    return !!resultJSON.result;
  }

  static async shareVideoAtUrl(videoUrl, stickerImageSourceOrUrl, stickerPosX, stickerPosY, attachmentUrl, caption) {
    const resolveAssetSource = require('react-native/Libraries/Image/resolveAssetSource');

    const resolvedSticker = resolveAssetSource(stickerImageSourceOrUrl);
    const result = await RNSnapchatKit.shareVideoAtUrl(
      videoUrl,
      resolvedSticker, resolvedSticker == null ? stickerImageSourceOrUrl : null,
      stickerPosX, stickerPosY,
      attachmentUrl,
      caption).catch(e => { reject(e) });
      const resultJSON = JSON.parse(result);
      if (!!resultJSON.error) {
        throw resultJSON.error
      }
      return !!resultJSON.result;
  }
}
