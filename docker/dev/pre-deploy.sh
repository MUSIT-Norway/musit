# More RAM than default was needed for the ElasticSearch container,
# the default was too little and made it stop.
sudo sysctl -w vm.max_map_count=262144
