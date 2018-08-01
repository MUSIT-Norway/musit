# More RAM than default was needed for the ElasticSearch container,
# the default was too little and made it stop.
sudo sysctl -w vm.max_map_count=262144

#Adds a new loopback address, a hack to let the nginx container talk to the node-js-backend running on localhost (on the container host)
#https://superuser.com/questions/1089092/how-to-add-a-second-local-loop-back-address
sudo ip addr add 192.168.40.1/32 dev lo
