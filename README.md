# Ludere
A LibRetro frontend for clean and simple emulation. Many retro emulators on Android are fragmented. The goal of Ludere is to make emulation consistent across multiple architectures while using a Bring Your Own Everything system. Users provide their own ROMs and cores, and Ludere handles the rest.

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

Here's how Ludere is configured:

```
└── Ludere
    ├── core (cached)
    ├── rom (cached)
    └── <rom-MD5>
        ├── sram
        ├── state-<0-4>
        └── tempstate
```

# Features
- BYOE system (Bring Your Own Everything), so no backend magic
- ROM and core are cached upon selection, so even if the files are moved from the internal storage, you can still play your last-cached ROM
- Five-slot save states
- Temporary states are saved and loaded to pick up where you left off seamlessly
- Android TV and external controller support
- Highly customizable in the settings

# Libraries
- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid): Our LibRetro frontend that interacts with RetroArch cores
- [RadialGamePad](https://github.com/Swordfish90/RadialGamePad): Intuitive touchscreen controls
- [LibRetro](http://buildbot.libretro.com/nightly/): Emulator cores for Android