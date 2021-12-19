# Autogen
Mass-package ROMs with the same config into individual APKs.

## Usage
1. Place roms and `config.xml` in the `input` directory (create it)
3. `./generate`
4. All APKs can be found in the `output` folder

The `input` folder can have subdirectories with their own configuration files. That means you can separate your ROMs into folders based on their core and include a config for each core in a separate folder. The Python script will recurse into each subdirectory with a `config.xml` file and build the ROMs accordingly.

## Example Structure
```
└── autogen
    ├── input
    │   ├── nes
    |   |   ├── config.xml
    |   |   └── *.nes
    │   ├── gba
    |   |   ├── config.xml
    |   |   └── *.gba
    │   └── snes
    |       ├── config.xml
    |       └── *.smc
    ├── output
    └── generate
```
