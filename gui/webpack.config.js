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
 
var Webpack = require('webpack');
var path = require('path');
var nodeModulesPath = path.resolve(__dirname, 'node_modules');
var buildPath = path.resolve(__dirname, 'build');
var autoPrefixer = require('autoprefixer');

module.exports = {
    devtool: 'eval',
    entry: [
        __dirname + '/app/main.js'
    ],
    output: {
        path: buildPath,
        filename: 'bundle.js',
        publicPath: '/build/',
        sourceMapFilename: 'bundle.map',
        historyApiFallback: true
    },
    devServer: {
        inline: true,
        port: 3333
    },
    resolve: {
        extensions: [
            '',
            '.js',
            '.jsx',
            '.json'
        ]
    },
    module: {
        loaders: [
            {
                test: /\.css$/,
                loader: 'style!css'
            },
            {
                test: /\.scss$/,
                loader: 'style!css!sass'
            },
            {
                test: /(\.js$)|(\.jsx$)/,
                exclude: /node_modules/,
                loader: ['babel'],
                query: {
                    presets: ['es2015', 'react']
                }
            },
            {
                test: /\.json$/,
                loaders: ['json']
            },
            {
                test: /\.(otf|eot|svg|ttf|woff)/,
                loader: 'url-loader?limit=10000'
            }
        ]
    },
    postcss: [autoPrefixer()]
};
