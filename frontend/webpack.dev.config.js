var webpack = require('webpack');
var autoprefixer = require('autoprefixer');

module.exports = {
  entry: [
    "webpack-dev-server/client?http://localhost:8000/",
    'webpack/hot/only-dev-server',
    'bootstrap-sass!./src/theme/bootstrap.config.js',
    'font-awesome-webpack!./src/theme/font-awesome.config.js',
    './src/client.js'
  ],
  output: {
    path: __dirname + '/public/assets',
    publicPath: '/assets/',
    filename: 'js/bundle.js'
  },
  plugins: [
    new webpack.DefinePlugin({
      __DEVELOPMENT__: true,
      __FAKE_FEIDE__: false, // <-------- DISABLE fake login selector
      __DEVTOOLS__: false  // <-------- DISABLE redux-devtools HERE
    }),
  ],
  module: {
    loaders: [
      { test: /\.js?$/, exclude: /node_modules/, loaders: ['react-hot', 'babel-loader', 'eslint-loader']},
      { test: /\.scss$/, loaders: ['style-loader', 'css-loader', 'postcss-loader', 'sass-loader'] },
      { test: /\.woff(\?v=\d+\.\d+\.\d+)?$/, loader: "url?limit=10000&mimetype=application/font-woff&name=css/[name]_[hash].[ext]" },
      { test: /\.woff2(\?v=\d+\.\d+\.\d+)?$/, loader: "url?limit=10000&mimetype=application/font-woff&name=css/[name]_[hash].[ext]" },
      { test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/, loader: "url?limit=10000&mimetype=application/octet-stream&name=css/[name]_[hash].[ext]" },
      { test: /\.eot(\?v=\d+\.\d+\.\d+)?$/, loader: "file?name=css/[name]_[hash].[ext]" },
      { test: /\.svg(\?v=\d+\.\d+\.\d+)?$/, loader: "url?limit=10000&mimetype=image/svg+xml&name=css/[name]_[hash].[ext]" },
      { test: /\.json$/, loader: 'json' },
      { test: /\.yaml/, loader: 'json!yaml' }
    ]
  },
  postcss: [ autoprefixer({ browsers: ['last 2 versions'] }) ]
}
