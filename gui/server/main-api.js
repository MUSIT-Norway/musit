#!/usr/bin/env node
if (process.env.NODE_ENV !== 'production') {
  if (!require('piping')({
    hook: true,
    ignore: /(\/\.|~$|\.json$)/i,
  })) {
    return;
  }
}
require('../server_babel_register'); // babel registration (runtime transpilation for node)
require('../app-api/server');
