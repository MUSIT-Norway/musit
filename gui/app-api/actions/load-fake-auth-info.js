export default function loadFakeAuthInfo(req) {
  return Promise.resolve(require('../fake_security.json').users || null);
}
