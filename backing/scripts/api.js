#!/usr/bin/env node
const config = require('../.babelConfig');
require('babel-register')(config);
require('../api/server');
