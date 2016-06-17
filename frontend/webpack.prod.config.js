var webpack = require('webpack');
var path = require('path');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var strip = require('strip-loader');
var autoprefixer = require('autoprefixer');
var WebpackStrip = require('strip-loader');

module.exports = {
  entry: [
    'bootstrap-sass!./src/theme/bootstrap.config.production.js',
    'font-awesome-webpack!./src/theme/font-awesome.config.production.js',
    './src/client.js'
  ],
  output: {
    path: __dirname + '/public/assets',
    publicPath: '/assets/',
    filename: 'js/bundle.js'
  },
  resolve: {
    modulesDirectories: [
      'src',
      'node_modules'
    ],
    extensions: [
      '',
      '.js',
      '.jsx',
      '.json'
    ]
  },
  progress: true,
  plugins: [
    new ExtractTextPlugin('css/bundle.css', {allChunks: true}),
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: '"production"'
      },
      __CLIENT__: true,
      __SERVER__: false,
      __DEVELOPMENT__: false,
      __FAKE_FEIDE__: false,
      __DEVTOOLS__: false  // <-------- DISABLE redux-devtools HERE
    }),
    // optimizations
    new webpack.optimize.DedupePlugin(),
    new webpack.optimize.OccurenceOrderPlugin(),
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        warnings: false
      }
    })
  ],
  module: {
    loaders: [
      { test: /\.js?$/, exclude: /node_modules/, loader: 'babel-loader'},
      { test: /\.js?$/, loader: WebpackStrip.loader('debug', 'console.log', 'console.error') },
      { test: /\.scss$/, loader: ExtractTextPlugin.extract('style', 'css-loader!postcss-loader!sass-loader') },
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
};
