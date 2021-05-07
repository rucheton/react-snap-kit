import { NativeModules, NativeEventEmitter } from "react-native";

export const RNSnapchatKit = NativeModules.SnapchatKit;
export const RNSnapchatKitEmitter = new NativeEventEmitter(RNSnapchatKit);

export default class SnapchatKit {
  static login() {
    return RNSnapchatKit.login();
  }

  static isLogged() {
    return RNSnapchatKit.isUserLoggedIn();
  }

  static async logout() {
    return RNSnapchatKit.logout();
  }

  static getUserInfo() {
    return RNSnapchatKit.fetchUserData();
  }

  static getAccessToken() {
    return RNSnapchatKit.getAccessToken();
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
