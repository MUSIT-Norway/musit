# Frontend and API Gateway

This directory contains the ReactJS w/redux and api gateway implementation,
for build and run information please read the README file from the project root.

Special thanks to Erik Rasmussen (https://github.com/erikras) for having
good examples available on his github, to move towards node and universal
rendring we used his react-redux-universal-hot-example as help.

If you experience ENOSPC errors please modify sysctl config with the following command:
echo fs.inotify.max_user_watches=524288 | sudo tee -a /etc/sysctl.conf && sudo sysctl -p

Note to self:
Update template with latest goodies from https://github.com/erikras/react-redux-universal-hot-example

Make sure its isometric and good for deployment, perhaps tune deployment size down (esp for the browser)
