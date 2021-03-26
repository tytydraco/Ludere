import os
import glob

for apk in glob.glob('output/*.apk'):
    os.system(f'adb install {apk}')
