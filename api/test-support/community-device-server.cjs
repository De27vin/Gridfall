// Local device-test harness, excluded from the production Docker image.
// Only Firebase verification is replaced; routes, validation and PostgreSQL are real.
const testUrl = process.env.COMMUNITY_TEST_DATABASE_URL;
if (process.env.NODE_ENV !== 'test' || process.env.COMMUNITY_DEVICE_TEST !== '1' ||
    !/^postgresql:\/\/[^/]+@127\.0\.0\.1:55439\/gridfall_community_test$/.test(testUrl || '') ||
    process.env.DATABASE_URL !== testUrl) {
  throw Error('Device harness requires the explicit disposable database and test opt-in');
}
process.env.COMMUNITY_DOWNLOAD_SECRET = 'disposable-device-test-only-secret-not-production';
const firebase = require('../dist/firebase');
firebase.verifyFirebaseToken = async token => {
  if (!['device-owner', 'device-player'].includes(token)) throw Error('Invalid test identity');
  return {firebaseUid: token, isAnonymous: false};
};
const {getOrCreateUserWithProfile} = require('../dist/services/usersService');
const {prisma} = require('../dist/db');
const {buildServer} = require('../dist/server');
(async () => {
  for (const name of ['device-owner', 'device-player']) {
    const {user} = await getOrCreateUserWithProfile({firebaseUid: name, isAnonymous: false});
    await prisma.user.update({where: {id: user.id}, data: {username: name, usernameNormalized: name}});
  }
  const app = buildServer();
  await app.listen({host: '127.0.0.1', port: 55440});
  console.log('Isolated device-test API ready on 127.0.0.1:55440');
  for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, async () => { await app.close(); process.exit(0); });
})().catch(error => { console.error(error); process.exit(1); });
