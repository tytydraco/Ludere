import xml.etree.ElementTree as ET
import os, shutil, sys, re

rootdir = os.path.abspath('..')
abis = ['armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64', 'universal']

if not os.path.isdir('../app/src/main/res/raw'):
    os.mkdir('../app/src/main/res/raw')

for subdir in [x[0] for x in os.walk('input')]:
    if not os.path.isfile(f'{subdir}/config.xml'):
        print(f'{subdir} does not contain a config.xml file. Skipping...')
        continue
    for file in os.listdir(subdir):
        if not os.path.isfile(f'{subdir}/{file}'):
            continue
        if file == '.gitignore':
            continue
        if file == 'config.xml':
            continue

        romname = os.path.splitext(file)[0].replace('\'', '')
        romext = os.path.splitext(file)[1]
        romid = romname.lower()
        romid = re.sub(r'[^A-Za-z0-9]+', '', romid)

        tree = ET.parse(f'{subdir}/config.xml')
        root = tree.getroot()

        shutil.copy(f'{subdir}/{file}', '../app/src/main/res/raw/rom')
        for element in root.iter('string'):
            if 'config_id' in element.attrib.get('name'):
                element.text = romid
            if 'config_name' in element.attrib.get('name'):
                element.text = romname
            if 'config_core' in element.attrib.get('name'):
                romcore = element.text
        tree.write('../app/src/main/res/values/config.xml')
        
        os.chdir('..')
        if os.name == 'nt':
            os.system('gradlew assembleRelease')
        else:
            os.system('./gradlew assembleRelease')
        os.chdir('autogen')
        
        for abi in abis:
            shutil.copy(f'../app/build/outputs/apk/release/app-{abi}-release.apk', f'output/{abi}/{romcore}_{romid}.apk')
