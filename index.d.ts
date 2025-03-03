import { ImageResolvedAssetSource } from "react-native";

declare module "react-native-snapchat-kit" {
  interface SnapchatUserData {
    displayName: string;
    externalId: string;
    avatar: string | null;
    accessToken: string;
    error?: any;
  }

  interface AssetOrURL {
    url?: string;
    asset?: ImageResolvedAssetSource;
  }

  interface Sticker {
    image: AssetOrURL;
    x?: number;
    y?: number;
  }

  export default class SnapchatKit {
    static login(): Promise<void>;
    static getAccessToken(): Promise<string>;
    static getUserInfo(): Promise<SnapchatUserData>;
    static isLogged(): Promise<boolean>;
    static logout(): Promise<boolean>;
    static sharePhoto(params: {
      photo?: AssetOrURL;
      sticker?: Sticker;
      attachment?: string;
      caption?: string;
    }): Promise<void>;
    static shareVideo(params: {
      url: string;
      sticker?: Sticker;
      attachment?: string;
      caption?: string;
    }): Promise<void>;
  }
}
