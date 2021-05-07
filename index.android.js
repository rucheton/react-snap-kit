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
        resolve();
      });
      const failedListener = this.addListener("LoginFailed", (res) => {
        succeededListener.remove();
        failedListener.remove();
        reject(res);
      });
      RNSnapchatKit.login();
    });
  }

  static getAccessToken() {
    return RNSnapchatKit.getAccessToken();
  }

  static async isLogged() {
    return await RNSnapchatKit.isUserLoggedIn();
  }

  static async logout() {
    return await RNSnapchatKit.logout();
  }

  static getUserInfo() {
    return RNSnapchatKit.fetchUserData();
  }

  static sharePhoto({ photo, sticker, attachment, caption }) {
    return RNSnapchatKit.sharePhotoResolved(
      photo && photo.asset,
      photo && photo.url,
      sticker && sticker.image.asset,
      sticker && sticker.image.url,
      (sticker && sticker.x) || 0.5,
      (sticker && sticker.y) || 0.5,
      attachment,
      caption
    );
  }

  static shareVideo({ url, sticker, attachment, caption }) {
    return RNSnapchatKit.shareVideoAtUrl(
      url,
      sticker && sticker.image.asset,
      sticker && sticker.image.url,
      (sticker && sticker.x) || 0.5,
      (sticker && sticker.y) || 0.5,
      attachment,
      caption
    );
  }
}
