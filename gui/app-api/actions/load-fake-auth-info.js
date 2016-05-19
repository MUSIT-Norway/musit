export default function loadFakeAuthInfo(req) {
  return Promise.resolve(require('../../app/fake_security.json').users || null);
}
