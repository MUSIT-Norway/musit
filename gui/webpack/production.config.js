/*
 *  MUSIT is a museum database to archive natural and cultural history data.
 *  Copyright (C) 2016  MUSIT Norway, part of www.uio.no (University of Oslo)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License,
 *  or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
require('babel-polyfill')

var webpack = require('webpack');
var path = require('path');
var CleanPlugin = require('clean-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var strip = require('strip-loader');

var projectRootPath = path.resolve(__dirname, '../');
var assetsPath = path.resolve(projectRootPath, './static/dist');

var WebpackIsomorphicToolsPlugin = require('webpack-isomorphic-tools/plugin');
var webpackIsomorphicToolsPlugin = new WebpackIsomorphicToolsPlugin(require('./webpack-isomorphic-tools'));

module.exports = {
    devtool: 'source-map',
    context: path.resolve(__dirname, '..'),
    entry: {
        'main': [
            'bootstrap-sass!./app/theme/bootstrap.config.production.js',
            'font-awesome-webpack!./app/theme/font-awesome.config.production.js',
            './app/client.js'
        ]
    },
    output: {
        path: assetsPath,
        filename: '[name]-[chunkhash].js',
        chunkFilename: '[name]-[chunkhash].js',
        publicPath: '/dist/'
    },
    resolve: {
        modulesDirectories: [
            'app',
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
        new CleanPlugin([assetsPath], {root: projectRootPath}),
        new ExtractTextPlugin('[name]-[chunkhash].css', {allChunks: true}),
        new webpack.DefinePlugin({
            'process.env': {
                NODE_ENV: '"production"'
            },
            __CLIENT__: true,
            __SERVER__: false,
            __DEVELOPMENT__: false,
            __DEVTOOLS__: false  // <-------- DISABLE redux-devtools HERE
        }),

        // optimizations
        new webpack.optimize.DedupePlugin(),
        new webpack.optimize.OccurenceOrderPlugin(),
        new webpack.optimize.UglifyJsPlugin({
          compress: {
            warnings: false
          }
        }),

        webpackIsomorphicToolsPlugin
    ],
    module: {
        loaders: [
            { test: /\.jsx?$/, exclude: /node_modules/, loaders: [strip.loader('debug'), 'babel']},
            { test: /\.json$/, loader: 'json-loader' },
            { test: /\.less$/, loader: ExtractTextPlugin.extract('style', 'css?modules&importLoaders=2&sourceMap!autoprefixer?browsers=last 2 version!less?outputStyle=expanded&sourceMap=true&sourceMapContents=true') },
            { test: /\.scss$/, loader: ExtractTextPlugin.extract('style', 'css?modules&importLoaders=2&sourceMap!autoprefixer?browsers=last 2 version!sass?outputStyle=expanded&sourceMap=true&sourceMapContents=true') },
            { test: /\.woff(\?v=\d+\.\d+\.\d+)?$/, loader: "url?limit=10000&mimetype=application/font-woff" },
            { test: /\.woff2(\?v=\d+\.\d+\.\d+)?$/, loader: "url?limit=10000&mimetype=application/font-woff" },
            { test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/, loader: "url?limit=10000&mimetype=application/octet-stream" },
            { test: /\.eot(\?v=\d+\.\d+\.\d+)?$/, loader: "file" },
            { test: /\.svg(\?v=\d+\.\d+\.\d+)?$/, loader: "url?limit=10000&mimetype=image/svg+xml" },
            { test: webpackIsomorphicToolsPlugin.regular_expression('images'), loader: 'url-loader?limit=10240' }
        ]
    }
};
