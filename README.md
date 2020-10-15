# LibRetroWrapper
A LibRetro-powered ROM packager for portable emulation

# Purpose
The goal of LibRetroWrapper is to increase the level of abstraction for emulation on Android.

Here's a diagram of how most Android emulators are configured:

```
└── Generic Emulator App
    ├── Roms
    │   ├── rom1.gba
    │   ├── rom2.gba
    │   └── rom3.gba
    ├── Saves
    │   ├── rom1.sav
    │   ├── rom2.sav
    │   └── rom3.sav
    └── States
        ├── rom1.state
        ├── rom2.state
        └── rom3.state
```

Here's how LibRetroWrapper is configured:

```
└── LibRetroDroid
    ├── rom.gba
    ├── rom.sav
    └── rom.state
```

# Features
- LibRetro core comes bundled at compile time, no external importing required
- ROM is packaged inside the APK, no external importing required
- Save state support (single slot)
- SRAM is dumped when the screen loses focus (sleep, go home, close)
- All-in-one package, can be easily distributed once packaged
- Android TV and external controller support

# Libraries
- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid): Our LibRetro frontend that interacts with RetroArch cores
- [RadialGamePad](https://github.com/Swordfish90/RadialGamePad): Intuitive touchscreen controls
- [RetroArch](http://buildbot.libretro.com/nightly/): LibRetro emulator cores for Android

# Configuration
- Copy `config/rom.properties.sample` to `config/rom.properties`
- Edit `config/rom.properties` and change your configuration
- Place your rom in `config/`, usually named `rom` (configurable)
- (Optional) Place your save state file at `config/save`
- (Optional) Place your SRAM save file at `config/state`

# Building
It is usually best to build a release build to reduce the total file size and improve performance. This method requires manual signing afterwards.
- `./gradlew assembleRelease` (don't forget to zipalign and apksigner)

For testing, simply use a debug build. It does not require any extra steps.
- `./gradlew assembleDebug`
