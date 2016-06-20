while [ ! -s src/client.js ]
  do
  printf ""
done
echo "found src"
while [ ! -s public/index.html ]
  do
  printf ""
done
echo "found public"
npm start
