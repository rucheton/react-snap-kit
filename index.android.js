import { NativeModules, NativeEventEmitter } from "react-native";

export const RNSnapchatKit = NativeModules.SnapchatKit;
export const RNSnapchatKitEmitter = new NativeEventEmitter(RNSnapchatKit);

export default class SnapchatKit {
  static addListener(eventType, listener, context) {
    return RNSnapchatKitEmitter.addListener(eventType, listener, context);
  }

  static login() {
    return new Promise((resolve, reject) => {
      const succeededListener = this.addListener("LoginSucceeded", (res) => {
        succeededListener.remove();
        failedListener.remove();
        this.getUserInfo().then(resolve).catch(reject);
      });
      const failedListener = this.addListener("LoginFailed", (res) => {
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
        .catch((e) => reject(e));
    });
  }

  static async sharePhoto({ photo, sticker, attachment, caption }) {
    const result = await RNSnapchatKit.sharePhotoResolved(
      photo && photo.asset,
      photo && photo.url,
      sticker && sticker.image.asset,
      sticker && sticker.image.url,
      (sticker && sticker.x) || 0.5,
      (sticker && sticker.y) || 0.5,
      attachment,
      caption
    ).catch((e) => {
      reject(e);
    });
    const resultJSON = JSON.parse(result);
    if (!!resultJSON.error) {
      throw resultJSON.error;
    }
    return !!resultJSON.result;
  }

  static async shareVideo({ url, sticker, attachment, caption }) {
    const result = await RNSnapchatKit.shareVideoAtUrl(
      url,
      sticker && sticker.image.asset,
      sticker && sticker.image.url,
      (sticker && sticker.x) || 0.5,
      (sticker && sticker.y) || 0.5,
      attachment,
      caption
    ).catch((e) => {
      reject(e);
    });
    const resultJSON = JSON.parse(result);
    if (!!resultJSON.error) {
      throw resultJSON.error;
    }
    return !!resultJSON.result;
  }
}
