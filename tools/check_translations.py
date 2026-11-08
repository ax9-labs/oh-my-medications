#!/usr/bin/env python3
"""Verify resource completeness and format arguments for every shipped translation."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parent.parent
res=root/'app/src/main/res'
def catalog(path):
    result={}
    for item in ET.parse(path).getroot():
        name=item.get('name')
        if name is None: continue
        assert name not in result, f'{path}: duplicate {name}'
        if item.get('translatable')=='false':continue
        result[name]=item
    return result
def formats(item):
    values=[item] if item.tag=='string' else list(item)
    return {tuple(sorted(re.findall(r'%(\d+)\$([ds])',''.join(v.itertext())))) for v in values}
base=catalog(res/'values/strings.xml')
assert base
for path in sorted(res.glob('values-*/strings.xml')):
    translated=catalog(path)
    assert base.keys()==translated.keys(),f'{path}: missing {base.keys()-translated.keys()}, unknown {translated.keys()-base.keys()}'
    for key,item in base.items():
        other=translated[key]
        assert other.tag==item.tag,f'{path}: resource type changed for {key}'
        assert formats(item)==formats(other),f'{path}: argument mismatch in {key}'
        assert ''.join(other.itertext()).strip(),f'{path}: empty {key}'
        if other.tag=='plurals':assert {'one','other'}<={v.get('quantity') for v in other},f'{path}: incomplete plural {key}'
    print(f'PASS: {path.parent.name}: {len(base)} translated resources and matching format arguments')

android='{http://schemas.android.com/apk/res/android}'
locales={x.get(android+'name') for x in ET.parse(res/'xml/locales_config.xml').getroot()}
arrays=ET.parse(res/'values/languages.xml').getroot()
tags=[x.text for x in arrays.find("string-array[@name='language_tags']")]
names=[x.text for x in arrays.find("string-array[@name='language_names']")]
assert tags[0]=='en' and set(tags)==locales and len(tags)==len(names)
assert ET.parse(res/'values/strings.xml').getroot().find("string[@name='app_name']").get('translatable')=='false'
print('PASS: app language choices match Android locale registration; English is the default')

# Any English prose still present in UI classes must be an explicitly internal diagnostic.
allowed={'No output stream','Alarm audio unavailable; vibration and stop notification remain active'}
for name in ['MainActivity','AlarmActivity','AlarmService','Reminders','MedAppearance','MedIconView','FloatingNavBar','HistoryExport','AlarmPermissions','ScheduleFeed','TimingText','UiKit','AdherenceChart','AnimatedLine']:
    source=(root/f'app/src/main/java/com/personal/medtracker/{name}.java').read_text()
    for token in re.findall(r'"(?:[^"\\]|\\.)*"',source):
        assert not re.search(r'[A-Za-z]{2,} [A-Za-z]{2,}',token[1:-1]) or token[1:-1] in allowed, f'{name}: hard-coded UI prose {token}'
print('PASS: UI, icon descriptions and notification classes contain no hard-coded English prose')
