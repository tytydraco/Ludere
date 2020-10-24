import xml.etree.ElementTree as ET
import os, shutil, sys, re

abspath = os.path.abspath(__file__)
dname = os.path.dirname(abspath)
os.chdir(dname)

shutil.copy('../app/src/main/res/values/config.xml', 'config.tmp.txt')
for file in os.listdir('input'):
    romname = os.path.splitext(file)[0]
    romext = os.path.splitext(file)[1]
    romid = re.sub(r'[^A-Za-z0-9]+', '', romname)

    tree = ET.parse('../app/src/main/res/values/config.xml')
    root = tree.getroot()

    shutil.copy(f'input/{file}', '../system/rom')
    for element in root.iter('string'):
        if 'config_rom_id' in element.attrib.get('name'):
            element.text = romid
        if 'config_name' in element.attrib.get('name'):
            element.text = romname
    tree.write('../app/src/main/res/values/config.xml')
    os.chdir('..')
    
    print(f' * Building {romname}...')
    if os.name is 'nt':
        os.system('gradlew assembleRelease')
    else:
        os.system('./gradlew assembleRelease')
    os.chdir(dname)

    shutil.copy('../app/build/outputs/apk/release/app-release.apk', f'output/{romid}.apk')

shutil.move('config.tmp.txt', '../app/src/main/res/values/config.xml')