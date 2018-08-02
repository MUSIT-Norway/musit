# More RAM than default was needed for the ElasticSearch container,
# the default was too little and made it stop.
sudo sysctl -w vm.max_map_count=262144

# Do this if you  want to have the nodejs backend running directly on the host, instead of in a containter (then you also need to change nginx.conf)
# Adds a new loopback address, a hack to let the nginx container talk to the node-js-backend running on localhost (on the container host)
# sudo ip addr add 192.168.40.1/32 dev lo
# https://superuser.com/questions/1089092/how-to-add-a-second-local-loop-back-address
