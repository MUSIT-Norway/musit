pushd ../../frontend
npm install
npm run build
popd
rm -rf public
mkdir -p public/assets
cp -R ../../frontend/public/assets/* public/assets/
rm -rf ../../frontend/public/assets/css/*
rm -rf ../../frontend/public/assets/js/*
cp ../../frontend/public/index.html public/
