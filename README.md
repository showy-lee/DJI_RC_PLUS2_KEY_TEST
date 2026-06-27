# DJI RC Plus 2 Key Test

这是一个用于 DJI RC Plus 2 遥控器按键、摇杆、拨轮和五维键检测的小工具。项目基于 DJI MSDK V5，当前主要目标是保留遥控器按键测试能力，同时尽量减小 APK 体积。

## 当前版本

- `applicationId`: `com.example.msdksample`
- `versionName`: `1.1`
- `versionCode`: `2`
- DJI SDK: `com.dji:dji-sdk-v5-aircraft:5.18.0`
- 当前 release APK: `app/build/outputs/apk/release/app-release.apk`
- 当前包体大小: `75,110,949` bytes，约 `71.6 MiB` / `75.1 MB`

## 功能

- DJI SDK 遥控器状态监听
  - 左右摇杆
  - 快门、录像、返航、暂停
  - C1/C2/C3、自定义/认证灯按键
  - 飞行模式档位
  - 左右拨轮、滚轮
  - 五维键

- Android `KeyEvent` 物理按键监听
  - 显示最近一次按键事件
  - 显示 `keyCode`、`scanCode`、`source`
  - 保留最近 8 条事件历史
  - 自动把新出现的 keyCode 加入界面观察列表

- ESP32 测试页
  - 使用左摇杆水平值映射舵机角度
  - 通过 Socket 向 ESP32 发送角度

## 已确认的 RC Plus 2 按键映射

设备侧 `/system/usr/keylayout/gpio-keys.kl` 将 DJI 物理 L1-L3/R1-R3 映射为 Android `F1-F6`：

| 物理按键 | Android keyCode |
| --- | --- |
| L1 | `KEYCODE_F1` / `131` |
| L2 | `KEYCODE_F2` / `132` |
| L3 | `KEYCODE_F3` / `133` |
| R1 | `KEYCODE_F4` / `134` |
| R2 | `KEYCODE_F5` / `135` |
| R3 | `KEYCODE_F6` / `136` |

另外，机身录像/拍照键来自 `DJI embedded joystick` 输入设备：

| 功能 | Android keyCode | scanCode |
| --- | --- | --- |
| Record | `KEYCODE_BUTTON_L1` / `102` | `310` |
| Shutter full press | `KEYCODE_BUTTON_R1` / `103` | `311` |

快门半按没有暴露为普通 Android `KeyEvent`。也尝试过 raw input、DJI RC Key、Camera Focus Key 和 joystick motion 诊断，均没有稳定可用事件；当前版本已移除这些半按快门实验代码。

## APK 瘦身策略

当前包体相比原始 DJI MSDK 示例已经明显缩小。主要处理如下：

- 移除 UX SDK 模块
  - `settings.gradle` 中不再 include `android-sdk-v5-uxsdk`
  - 不打包地图、动画、图标等 UX 组件

- 仅保留 `arm64-v8a`
  - `abiFilters 'arm64-v8a'`
  - RC Plus 2 使用 arm64，避免打包多 ABI so

- 限制资源语言
  - `resourceConfigurations += ['en', 'zh-rCN']`

- 排除当前测试工具不需要的 native 库
  - FFmpeg: `libavcodec.so`、`libavformat.so`、`libswscale.so` 等
  - MRTC/直播: `libmrtc_*`、`libndi.so`
  - 云接入/PPAL: `libcloud_access_jni.so`、`libPPAL.so`、`libppal-jni.so`
  - Agora: `libagora-*`

保留的 DJI 核心库不要继续盲删，否则容易导致 SDK 注册、遥控器 Key 监听或应用启动失败。

## 构建

```bash
./gradlew :app:assembleRelease
```

Windows PowerShell:

```powershell
.\gradlew :app:assembleRelease
```

构建产物：

```text
app/build/outputs/apk/release/app-release.apk
```

当前 release 使用 debug signing config 签名，方便直接安装到测试遥控器。

## 安装

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

启动主测试页：

```bash
adb shell am start -n com.example.msdksample/.JoystickTestActivity
```

## 已知限制

- 快门半按目前无法在第三方 APK 中可靠捕获。
- APK 仍然较大，主要由 DJI MSDK 核心 native 库决定。
- ESP32 页面的中文注释/部分字符串来自原项目，后续可以单独整理编码和文案。
