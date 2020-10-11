import urllib.request
import os
import pathlib
import shutil
import sys
from zipfile import ZipFile

LATEST_CORES = "http://buildbot.libretro.com/nightly/android/latest"
ABIS = ["arm64-v8a", "armeabi-v7a", "x86", "x86_64"]

if (len(sys.argv) < 2):
    print("Missing core name")
    exit(1)

CORE = sys.argv[1]

for abi in ABIS:
    print(f"FETCHING {CORE} FOR {abi}")
    url = f"{LATEST_CORES}/{abi}/{CORE}_libretro_android.so.zip"
    core_path = f"app/src/main/jniLibs/{abi}"
    zip_path = f"{core_path}/{CORE}"

    if (os.path.isdir(core_path)):
        shutil.rmtree(core_path)

    pathlib.Path(core_path).mkdir(parents=True)
    urllib.request.urlretrieve(url, zip_path)
    
    with ZipFile(zip_path, "r") as zip:
        zip.extractall(core_path)
    os.remove(zip_path)
