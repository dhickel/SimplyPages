// Explicit test-client security setup; never retries or replays a mutation.
async function sandboxRequest(page, initializePath = '/editing-demo') {
    const initial = await page.request.get(initializePath);
    if (initial.status() !== 200) throw new Error(`Sandbox initialization failed: ${initial.status()}`);
    const tokenResponse = await page.request.get('/sandbox/token');
    const {token} = await tokenResponse.json();
    const origin = new URL(tokenResponse.url()).origin;
    const options = input => ({...input, headers:{...input?.headers, Origin:origin, 'X-Sandbox-CSRF':token}});
    return {
        get: (...args) => page.request.get(...args),
        post: (url, input) => page.request.post(url, options(input)),
        delete: (url, input) => page.request.delete(url, options(input))
    };
}
module.exports = {sandboxRequest};
