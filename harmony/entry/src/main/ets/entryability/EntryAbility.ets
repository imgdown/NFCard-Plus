import { AbilityConstant, UIAbility, Want } from '@kit.AbilityKit';
import { window } from '@kit.ArkUI';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index');
  }

  onWindowStageDestroy(): void {
  }

  onForeground(): void {
    // TODO: 注册 NFC 标签发现回调（@kit.ConnectivityKit nfcTag）
  }

  onBackground(): void {
    // TODO: 注销 NFC 标签发现回调
  }
}
