#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

./mvnw -pl site -am clean package -DskipTests

python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile

jar = Path('site/target/simplypages-site.jar')
sources = {
    'docs': Path('docs'),
    'javadocs': Path('simplypages/target/reports/apidocs'),
}
required = {'docs': 'README.md', 'javadocs': 'index.html'}
with ZipFile(jar) as archive:
    names = archive.namelist()
    if len(names) != len(set(names)):
        raise SystemExit('Duplicate entries in site JAR')
    for kind, source in sources.items():
        expected = {p.relative_to(source).as_posix(): p for p in source.rglob('*') if p.is_file()}
        if required[kind] not in expected:
            raise SystemExit(f'Missing required {kind} source: {source / required[kind]}')
        prefix = f'BOOT-INF/classes/static/{kind}/'
        actual = {name[len(prefix):] for name in names if name.startswith(prefix) and not name.endswith('/')}
        if actual != expected.keys():
            raise SystemExit(f'{kind} inventory mismatch: missing={sorted(expected.keys() - actual)}, extra={sorted(actual - expected.keys())}')
        for name, path in expected.items():
            if archive.read(prefix + name) != path.read_bytes():
                raise SystemExit(f'{kind} content mismatch: {name}')
        print(f'Verified {len(expected)} {kind} resources')
print(f'Runnable artifact: {jar.resolve()}')
PY
