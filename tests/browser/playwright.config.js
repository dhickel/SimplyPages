const path = require('node:path');
const {defineConfig} = require('playwright/test');

module.exports = defineConfig({
    use: {baseURL: 'http://127.0.0.1:18080'},
    webServer: {
        command: 'java -jar demo/target/simplypages-demo.jar --server.address=127.0.0.1 --server.port=18080',
        cwd: path.resolve(__dirname, '..', '..'),
        url: 'http://127.0.0.1:18080/editing-demo',
        reuseExistingServer: false,
        timeout: 60000
    }
});
