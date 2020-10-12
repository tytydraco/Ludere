# LibretroWrapper
A LibRetro-powered ROM packager for portable emulation

# Libraries
- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid): Our LibRetro frontend that interacts with RetroArch cores
- [RadialGamePad](https://github.com/Swordfish90/RadialGamePad): Intuitive touchscreen controls
- [RetroArch](http://buildbot.libretro.com/nightly/): LibRetro emulator cores for Android

# Building
- Place your rom at `app/src/main/res/raw/rom`
- Edit `gradle.properties`
- Navigate down to `Rom Wrapper Information` and change it accordingly
- Build

# GitHub Action
- Fork this repository
- Make it private (avoid leaking your ROMs)
- Head over to your repo and click Actions
- Click Packager, then Run workflow
- Configure and run
