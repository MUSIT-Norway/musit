export default function login(req) {
  console.log(req.body);
  const user = {
    name: req.body.name,
  };
  req.session.user = user;
  return Promise.resolve(user);
}
