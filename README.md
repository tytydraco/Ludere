# LibretroWrapper
A LibRetro-powered ROM packager for portable emulation

# Libraries
- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid): Our LibRetro frontend that interacts with RetroArch cores
- [RadialGamePad](https://github.com/Swordfish90/RadialGamePad): Intuitive touchscreen controls
- [RetroArch](http://buildbot.libretro.com/nightly/): LibRetro emulator cores for Android

# Configuration
- Copy `rom.properties.sample` to `rom.properties`
- Edit `rom.properties` and change your configuration

# Building
- Place your rom at `app/src/main/res/raw/rom`
- `./gradlew assembleRelease` (don't forget to zipalign and apksigner)
