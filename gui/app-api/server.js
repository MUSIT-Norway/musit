import express from 'express'
import session from 'express-session'
import bodyParser from 'body-parser'
import config from '../app/config'
import * as actions from './actions/index'
import { mapUrl } from './utils/url.js'
import PrettyError from 'pretty-error'
import http from 'http'
import SocketIo from 'socket.io'
import Passport from 'passport'
import {Strategy as DataportenStrategy} from 'passport-dataporten'

const pretty = new PrettyError()
const app = express()
const dataportenCallbackUrl = `https://${config.apiHost}:${config.apiPort}/auth/dataporten/callback`

const server = new http.Server(app)

// TODO: Consider placing this initialization strategy into the config object
passport.use(new DataportenStrategy({
    clientID: config.dataportenClientID,
    clientSecret: config.dataportenClientSecret,
    callbackURL: dataportenCallbackUrl
  },
  (accessToken, refreshToken, profile, done) => {
    //load user and return done with the user in it.
    return done(err, {username: 'foo'})
  }
))

const io = new SocketIo(server)
io.path('/ws')

app.use(session({
  secret: 'react and redux rule!!!!',
  resave: false,
  saveUninitialized: false,
  cookie: { maxAge: 60000 }
}));
app.use(bodyParser.json())


app.use((req, res) => {
  passport.authorize('dataporten', { failureRedirect: '/' }), (req, res) => {
    res.redirect('/')
  })
  const splittedUrlPath = req.url.split('?')[0].split('/').slice(1)

  const { action, params } = mapUrl(actions, splittedUrlPath)

  if (action) {
    action(req, params)
      .then((result) => {
        if (result instanceof Function) {
          result(res)
        } else {
          res.json(result)
        }
      }, (reason) => {
        if (reason && reason.redirect) {
          res.redirect(reason.redirect)
        } else {
          console.error('API ERROR:', pretty.render(reason))
          res.status(reason.status || 500).json(reason)
        }
      });
  } else {
    res.status(404).end('NOT FOUND')
  }
})


const bufferSize = 100
const messageBuffer = new Array(bufferSize)
let messageIndex = 0

if (config.apiPort) {
  const runnable = app.listen(config.apiPort, (err) => {
    if (err) {
      console.error(err)
    }
    console.info('----\n==> 🌎  API is running on port %s', config.apiPort)
    console.info('==> 💻  Send requests to http://%s:%s', config.apiHost, config.apiPort)
  });

  io.on('connection', (socket) => {
    socket.emit('news', { msg: `'Hello World!' from server` })

    socket.on('history', () => {
      for (let index = 0; index < bufferSize; index++) {
        const msgNo = (messageIndex + index) % bufferSize
        const msg = messageBuffer[msgNo]
        if (msg) {
          socket.emit('msg', msg)
        }
      }
    });

    socket.on('msg', (data) => {
      data.id = messageIndex
      messageBuffer[messageIndex % bufferSize] = data
      messageIndex++
      io.emit('msg', data)
    })
  })
  io.listen(runnable)
} else {
  console.error('==>     ERROR: No PORT environment variable has been specified')
}
