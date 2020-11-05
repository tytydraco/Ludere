import xml.etree.ElementTree as ET
import os, shutil, sys, re

scriptabspath = os.path.abspath(__file__)
cwdabspath = os.path.dirname(scriptabspath)
os.chdir(cwdabspath)

rootdir = os.path.abspath('..')

if not os.path.isdir(f'{rootdir}/system'):
    os.mkdir(f'{rootdir}/system')

shutil.copy(f'{rootdir}/app/src/main/res/values/config.xml', f'{cwdabspath}/config.tmp.txt')

buildsize = 0
for subdir in [x[0] for x in os.walk(f'{cwdabspath}/input')]:
    if not os.path.isfile(f'{subdir}/config.xml'):
        continue
    for file in os.listdir(subdir):
        if not os.path.isfile(f'{subdir}/{file}'):
            continue
        if file == '.gitignore':
            continue
        if file == 'config.xml':
            continue
        buildsize += 1

for subdir in [x[0] for x in os.walk(f'{cwdabspath}/input')]:
    if not os.path.isfile(f'{subdir}/config.xml'):
        continue
    for file in os.listdir(subdir):
        if not os.path.isfile(f'{subdir}/{file}'):
            continue
        if file == '.gitignore':
            continue
        if file == 'config.xml':
            continue

        print('')
        print(f' *****************************************')
        print(f' * BUILDS REMAINING: {buildsize}')
        print(f' * BUILDING: {file}')
        print(f' *****************************************')
        print('')

        romname = os.path.splitext(file)[0]
        romext = os.path.splitext(file)[1]
        romid = re.sub(r'[^A-Za-z0-9]+', '', romname)

        tree = ET.parse(f'{subdir}/config.xml')
        root = tree.getroot()

        shutil.copy(f'{subdir}/{file}', f'{rootdir}/app/src/main/res/raw/rom')
        for element in root.iter('string'):
            if 'config_id' in element.attrib.get('name'):
                element.text = romid
            if 'config_name' in element.attrib.get('name'):
                element.text = romname
            if 'config_core' in element.attrib.get('name'):
                romcore = element.text
        tree.write(f'{rootdir}/app/src/main/res/values/config.xml')
        
        os.chdir(rootdir)
        os.system(f'{rootdir}/gradlew assembleRelease')
        os.chdir(cwdabspath)
        
        shutil.copy(f'{rootdir}/app/build/outputs/apk/release/app-release.apk', f'{cwdabspath}/output/{romcore}_{romid}.apk')

        buildsize -= 1
shutil.move(f'{cwdabspath}/config.tmp.txt', f'{rootdir}/app/src/main/res/values/config.xml')
