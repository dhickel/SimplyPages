#!/usr/bin/env node

"use strict";

const { access } = require("fs/promises");
const { spawn, exec } = require("child_process");
const fs = require("fs");
const https = require("https");
const { join } = require("path");
const path = require("path");
const zlib = require("zlib");
const os = require("os");

const tar = require("tar");

const lifecycleEvent = process.env.npm_lifecycle_event;
const installPath = path.join(os.tmpdir(), "jules_tmp")
const version = "v0.1.42";

const BINARY_NAMES = process.platform === "win32"
    ? ["jules.exe", "run.cjs"]
    : ["jules", "run.cjs"];
const URL =
    "https://storage.googleapis.com/jules-cli/{{version}}/jules_external_{{version}}_{{platform}}_{{arch}}.tar.gz";

/**
 * Mapping from Node's `process.arch` to Go's `$GOARCH`.
 * @type {Object.<string, string>}
 */
const ARCH_MAPPING = {
    x64: "amd64",
    arm64: "arm64",
};

/**
 * Mapping from Node's `process.platform` to Go's `$GOOS`.
 * @type {Object.<string, string>}
 */
const PLATFORM_MAPPING = {
    darwin: "darwin",
    linux: "linux",
    win32: "windows",
};

/**
 * Constructs the binary's download URL.
 * @returns {{url: string}}
 */
function getURL() {
    if (!ARCH_MAPPING[process.arch]) {
        throw new Error(`Unsupported architecture: ${process.arch}`);
    }
    if (!PLATFORM_MAPPING[process.platform]) {
        throw new Error(`Unsupported platform: ${process.platform}`);
    }

    const finalURL = URL.replace(/{{arch}}/g, ARCH_MAPPING[process.arch])
        .replace(/{{platform}}/g, PLATFORM_MAPPING[process.platform])
        .replace(/{{version}}/g, version);

    return { url: finalURL };
}

/**
 * Downloads a file from a URL, handling redirects.
 * @param {string} url - The URL to download from.
 * @param {string} destinationPath - The local path to save the file.
 * @returns {Promise<void>}
 */
function downloadFromUrl(url, destinationPath) {
    return new Promise((resolve, reject) => {
        const request = https.get(url, (response) => {
            if (
                response.statusCode >= 300 &&
                response.statusCode < 400 &&
                response.headers.location
            ) {
                return downloadFromUrl(
                    response.headers.location,
                    destinationPath,
                )
                    .then(resolve)
                    .catch(reject);
            }

            if (response.statusCode !== 200) {
                return reject(
                    new Error(
                        `Download failed with status code: ${response.statusCode}`,
                    ),
                );
            }

            const fileStream = fs.createWriteStream(destinationPath);
            response.pipe(fileStream);

            fileStream.on("finish", () => {
                fileStream.close(resolve); // close() is async, resolve in its callback
            });

            fileStream.on("error", (err) => {
                fs.unlink(destinationPath, () => reject(err)); // Delete the partial file on error
            });
        });

        request.on("error", (err) => {
            reject(err);
        });
    });
}

/**
 * Extracts a specific binary from a local .tar.gz file.
 * @param {string} tarballPath - The local path to the .tar.gz file.
 * @param {string} destinationPath - The destination directory for the binary.
 * @returns {Promise<void>}
 */
function extractTarball(tarballPath, destinationPath) {
    return new Promise((resolve, reject) => {
        const stream = fs
            .createReadStream(tarballPath)
            .pipe(zlib.createGunzip()) // Decompress the gzip stream
            .pipe(
                // Extract only the specified binary file to the destination directory
                tar.extract({ cwd: destinationPath }, BINARY_NAMES),
            );

        // Handle successful extraction
        stream.on("finish", () => {
            for (const name of BINARY_NAMES) {
                const finalBinPath = path.join(destinationPath, name);
                // Set executable permissions (e.g., rwxr-xr-x) on Unix-like systems ONLY
                if (process.platform !== "win32") {
                    fs.chmodSync(finalBinPath, 0o755);
                }
            }
            resolve();
        });

        // Handle errors during the stream process
        stream.on("error", reject);
    });
}

// --- Actions ---

/**
 * Installs the Go binary.
 * @returns {Promise<void>}
 */
async function install(installPath) {
    const { url } = getURL();
    fs.mkdirSync(installPath, { recursive: true });
    const localTarballName = path.basename(url);
    let localTarballPath = path.join(installPath, localTarballName);
    try {
        await downloadFromUrl(url, localTarballPath);
        await extractTarball(localTarballPath, installPath);
    } catch (error) {
        console.error(
            "\n❌ A critical error occurred during the process.",
            error,
        );
    } finally {
        if (fs.existsSync(localTarballPath)) {
            fs.unlinkSync(localTarballPath);
        }
    }
}

async function findFileInCurrentDir(fileName, searchPaths) {
    for (const dirPath of searchPaths) {
        // Construct the full path using path.join for cross-platform compatibility
        const fullPath = join(__dirname, dirPath, fileName);

        try {
            await access(fullPath);
            // Fix for PNPM:Check if it's actually a binary and not a shell script wrapper
            // Read the first few bytes to check if it's a shell script
            const buffer = Buffer.alloc(20);
            const fd = fs.openSync(fullPath, 'r');
            fs.readSync(fd, buffer, 0, 20, 0);
            fs.closeSync(fd);
            const header = buffer.toString('utf8', 0, 20);
            // Skip if it's a shell script (starts with #!/bin/sh or #!/usr/bin/env)
            if (header.startsWith('#!/bin/sh') || header.startsWith('#!/usr/bin/env')) {
                continue;
            }
            return fullPath;
        } catch (error) {
            continue;
        }
    }
    return null;
}

(async () => {
    // The `dist` paths are for pnpm, but we are actually not supporting pnpm
    let binPath = await findFileInCurrentDir("jules", [
        "./",
        "./dist",
        "node_modules/.bin",
        "node_modules/.bin/dist",
    ]);

    if (binPath != null || lifecycleEvent === "postinstall") {
        // It's already installed
    } else {
        // It's invoked by `npx` or `node`
        fs.mkdirSync(installPath, { recursive: true });
        await install(installPath);
        const binaryName = process.platform === "win32" ? "jules.exe" : "jules";
        binPath = path.join(installPath, binaryName);
    }

    const child = spawn(binPath, process.argv.slice(2), {
        stdio: "inherit",
    });

    child.on("error", (err) => {
        console.error("Failed to start jules binary:", err);
        process.exit(1);
    });

    child.on("exit", (code, signal) => {
        if (signal) {
            process.exit(1);
        } else {
            process.exit(code || 0);
        }
    });
})();
