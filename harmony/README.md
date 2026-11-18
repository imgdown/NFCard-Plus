# NFCard-Plus 鸿蒙（HarmonyOS NEXT）工程骨架

本目录为鸿蒙 NEXT 应用的 ArkTS 工程骨架，**第一版不参与 CI 打包**，供后续版本开发使用。

## 现状

- 已搭建：应用级配置（AppScope）、entry 模块、三栏界面骨架（自动读取 / 手动读取 / 关于）。
- 待开发：NFC 读卡能力（`@kit.ConnectivityKit` 的 `nfcTag` 模块，IsoDep 对应 `IsoDepTag`）、
  协议层移植（可参考 `core-protocol` 的 Kotlin 实现逐文件翻译为 ArkTS）。

## 开发环境

- DevEco Studio 5.0+（HarmonyOS NEXT，API 12+）
- 真机调试与发布需在华为开发者联盟申请调试/发布证书与 Profile，
  在 DevEco Studio 的 Signing Configs 中配置（证书材料不要提交到仓库）。

## 为什么第一版不打包鸿蒙

鸿蒙 NEXT 的 HAP/APP 打包依赖 DevEco 工具链（hvigor）与华为签名证书，
无法在 GitHub Actions 中以当前条件自动产出可安装包；且 NFC 读卡接口与安卓差异较大，
需要单独适配与真机验证。路线图见仓库根 README。

## 目录结构

```
harmony/
├── AppScope/                  # 应用级配置与资源
├── build-profile.json5        # 工程级构建配置
├── hvigorfile.ts              # 工程级构建脚本
├── oh-package.json5           # 工程级依赖
└── entry/                     # 主模块
    ├── src/main/ets/
    │   ├── entryability/      # 入口 Ability
    │   └── pages/Index.ets    # 三栏界面骨架
    └── src/main/module.json5  # 模块配置
```

---

内容由 AI 生成
