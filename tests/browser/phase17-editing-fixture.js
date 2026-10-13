const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const net = require('node:net');
const crypto = require('node:crypto');
const {spawn, spawnSync} = require('node:child_process');
const {test: base, expect, request: apiRequest} = require('playwright/test');

const root = path.resolve(__dirname, '../..');
const fixtureName = 'io.mindspice.demo.integration.Phase17EditingBrowserFixture';
const classDirectory = 'io/mindspice/demo/integration';
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');

// Only packaged production classes/resources and this launcher enter the runtime classpath.
// In particular, target/test-classes (and its Spring test configurations) is NEVER scanned.
function unpack(jar, directory) {
    const result = spawnSync('python3', ['-c', String.raw`
import hashlib, json, pathlib, sys, zipfile
jar, destination, root = map(pathlib.Path, sys.argv[1:])
sha = lambda b: hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(jar) as archive:
    entries = archive.namelist()
    assert not any('Phase17EditingBrowserFixture' in n for n in entries), 'Fixture shipped in product'
    for module in ('demo', 'simplypages'):
        for folder in ('src/main/java', 'src/main/resources'):
            for p in (root/module/folder).rglob('*'):
                if p.is_file() and p.suffix != '.md':
                    assert p.stat().st_mtime <= jar.stat().st_mtime, 'Stale product source: '+str(p)
    classes = [n for n in entries if n.startswith('BOOT-INF/classes/') and n.endswith('.class')]
    assert len(classes) == 61, 'Unexpected production class set'
    for n in classes:
        assert archive.read(n) == (root/'demo/target/classes'/n.removeprefix('BOOT-INF/classes/')).read_bytes(), n
    library = archive.read('BOOT-INF/lib/simplypages-1.1.0a.jar')
    assert library == (root/'simplypages/target/simplypages-1.1.0a.jar').read_bytes(), 'Stale nested library'
    resources = list((root/'demo/src/main/resources').rglob('*'))
    for p in resources:
        if p.is_file():
            n = 'BOOT-INF/classes/'+str(p.relative_to(root/'demo/src/main/resources'))
            assert archive.read(n) == p.read_bytes(), n
    selected = [n for n in entries if not n.endswith('/') and (n.startswith('BOOT-INF/classes/') or n.startswith('BOOT-INF/lib/'))]
    for n in selected:
        target = destination/n
        assert target.resolve().is_relative_to(destination.resolve()), 'Unsafe archive path'
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(archive.read(n))
    print(json.dumps(dict(jarSHA=sha(jar.read_bytes()), productionClasses=len(classes),
        classBytesExact=True, nestedLibrarySHA=sha(library), demoResourcesExact=sum(p.is_file() for p in resources),
        packagedFixture=False, extractedEntries=len(selected))))
`, jar, directory, root], {encoding: 'utf8'});
    if (result.status !== 0) throw new Error(`Package fidelity refused fixture startup:\n${result.stdout}\n${result.stderr}`);
    const proof = JSON.parse(result.stdout);
    const source = path.join(root, 'demo/src/test/java', classDirectory, 'Phase17EditingBrowserFixture.java');
    const compiled = path.join(root, 'demo/target/test-classes', classDirectory);
    const names = fs.readdirSync(compiled).filter(name => /^Phase17EditingBrowserFixture(?:\$.*)?\.class$/.test(name));
    if (!names.includes('Phase17EditingBrowserFixture.class')) throw new Error('Run Maven test-compile before browser fixtures');
    const onlyFixture = path.join(directory, 'fixture-only', classDirectory);
    fs.mkdirSync(onlyFixture, {recursive: true});
    proof.fixtureClassHashes = {};
    for (const name of names) {
        const input = path.join(compiled, name);
        if (fs.statSync(input).mtimeMs < fs.statSync(source).mtimeMs) throw new Error('Stale compiled browser fixture');
        const bytes = fs.readFileSync(input);
        fs.writeFileSync(path.join(onlyFixture, name), bytes, {flag: 'wx'});
        proof.fixtureClassHashes[name] = sha(bytes);
    }
    proof.fixtureSourceSHA = sha(fs.readFileSync(source));
    proof.safeClasspath = true;
    return proof;
}

async function freePort(workerIndex) {
    const start = 19000 + (workerIndex % 50) * 16;
    for (let offset = 0; offset < 1000; offset++) {
        const port = 19000 + ((start - 19000 + offset) % 1000);
        const free = await new Promise((resolve, reject) => {
            const probe = net.createServer();
            probe.once('error', error => error.code === 'EADDRINUSE' ? resolve(false) : reject(error));
            probe.listen(port, '127.0.0.1', () => probe.close(() => resolve(true)));
        });
        if (free) return port;
    }
    throw new Error('No free owned fixture port; refusing existing servers');
}

async function launch(options, testInfo) {
    const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'phase17-senior117-fixture-'));
    const token = crypto.randomBytes(16).toString('hex');
    const evidence = `/tmp/phase17-senior117-fixture-${token}`;
    let child, request, descriptor, exited, stopped = false;
    let proof = {test: testInfo.title, worker: testInfo.workerIndex, token};
    async function stop() {
        if (stopped) return proof.seedAtShutdown;
        stopped = true;
        try {
            if (request) await request.dispose();
        } finally {
            try {
                if (child) {
                    child.stdin.end();
                    const term = setTimeout(() => child.kill('SIGTERM'), 10000);
                    const kill = setTimeout(() => child.kill('SIGKILL'), 20000);
                    try { await exited; } finally { clearTimeout(term); clearTimeout(kill); }
                    proof.exitCode = child.exitCode;
                    proof.exitSignal = child.signalCode;
                    const snapshot = fs.readFileSync(evidence + '.log', 'utf8').split('\n')
                        .find(line => line.startsWith('PHASE17_FIXTURE_STATE '));
                    if (snapshot) proof.seedAtShutdown = JSON.parse(snapshot.slice('PHASE17_FIXTURE_STATE '.length));
                }
            } finally {
                if (descriptor !== undefined) fs.closeSync(descriptor);
                fs.rmSync(directory, {recursive: true, force: true});
                proof.ownedProcessStopped = !child || child.exitCode !== null || child.signalCode !== null;
                proof.temporaryClasspathRemoved = !fs.existsSync(directory);
                fs.writeFileSync(evidence + '.json', JSON.stringify(proof, null, 2), {flag: 'wx'});
            }
        }
        if (proof.readinessOwnedToken && (proof.exitCode !== 0 || !proof.seedAtShutdown)) {
            throw new Error(`Fixture did not shut down cleanly with an actual seed snapshot: ${evidence}.log`);
        }
        return proof.seedAtShutdown;
    }
    try {
        const jar = path.resolve(process.env.PHASE17_DEMO_JAR || path.join(root, 'demo/target/simplypages-demo.jar'));
        proof = {...proof, ...unpack(jar, directory)};
        const port = await freePort(testInfo.workerIndex);
        proof.port = port;
        proof.arrangedSeed = {id: 'module-9', contentSHA: sha(Buffer.from(options.content ?? 'Alpha,Beta,Gamma')),
            canEdit: options.canEdit !== false, canDelete: options.canDelete !== false, mode: options.mode ?? 'OWNER_EDIT'};
        descriptor = fs.openSync(evidence + '.log', 'wx');
        const classpath = [path.join(directory, 'fixture-only'), path.join(directory, 'BOOT-INF/classes'),
            path.join(directory, 'BOOT-INF/lib/*')].join(path.delimiter);
        const java = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin/java') : 'java';
        child = spawn(java, ['-cp', classpath, fixtureName, String(port),
            Buffer.from(options.content ?? 'Alpha,Beta,Gamma', 'utf8').toString('base64'),
            String(options.canEdit !== false), String(options.canDelete !== false), options.mode ?? 'OWNER_EDIT', token],
        {cwd: root, stdio: ['pipe', descriptor, descriptor]});
        let spawnFailure;
        exited = new Promise(resolve => {
            child.once('error', error => {spawnFailure = error; resolve();});
            child.once('exit', resolve);
        });
        const baseURL = `http://127.0.0.1:${port}`;
        let ready = false;
        for (let attempt = 0; attempt < 600; attempt++) {
            if (spawnFailure) throw spawnFailure;
            if (child.exitCode !== null || child.signalCode !== null) throw new Error(`Fixture exited: ${evidence}.log`);
            if (fs.readFileSync(evidence + '.log', 'utf8').includes(`PHASE17_FIXTURE_READY ${token} ${port}`)) {
                ready = true;
                break;
            }
            await sleep(100);
        }
        if (!ready) throw new Error(`Owned fixture startup timed out: ${evidence}.log`);
        request = await apiRequest.newContext({baseURL});
        expect((await request.get('/editing-demo')).status()).toBe(200);
        proof.readinessOwnedToken = true;
        console.log(JSON.stringify({fixture: testInfo.title, jarSHA: proof.jarSHA, port, evidence}));
        return {id: 'module-9', request, baseURL, stop};
    } catch (error) {
        proof.startupFailure = error.message;
        await stop();
        throw error;
    }
}

const test = base.extend({
    editingFixture: async ({}, use, testInfo) => {
        const owned = [];
        try {
            await use(async (options = {}) => {
                const fixture = await launch(options, testInfo);
                owned.push(fixture);
                return fixture;
            });
        } finally {
            for (const fixture of owned.reverse()) await fixture.stop();
        }
    }
});
module.exports = {test, expect};
