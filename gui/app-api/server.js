import express from 'express'
import session from 'express-session'
import bodyParser from 'body-parser'
import config from '../app/config'
import { FAKE_STRATEGY } from '../app/config'
import * as actions from './actions/index'
import { mapUrl } from './utils/url.js'
import PrettyError from 'pretty-error'
import http from 'http'
import SocketIo from 'socket.io'
import APIGateway from './gateway'
import Passport from 'passport'
import {Strategy as DataportenStrategy} from 'passport-dataporten'
import {Strategy as JsonStrategy} from 'passport-json-custom'

const pretty = new PrettyError()
const app = express()
app.locals.gateway = new APIGateway('/api')

const server = new http.Server(app)

const io = new SocketIo(server)
io.path('/ws')

/* *** START Security *** */
const dataportenUri = '/auth/dataporten'
const dataportenCallbackUri = '/auth/dataporten/callback'
const dataportenCallbackUrl = `http://${config.host}:8080/api${dataportenCallbackUri}`
var passportStrategy = null
var passportLoginType = null
console.log(dataportenCallbackUrl)

if (config.FAKE_STRATEGY === config.dataportenClientSecret) {
  // TODO:FAKEIT
  console.log(' Installing strategy: LOCAL')
  passportLoginType = 'json-custom'

  const findUser = (username) => {
    const securityDatabase = require('./fake_security.json')
    return securityDatabase.users.find( (user) => user.userId == username)
  }

  const localCallback = (credentials, done) => {
    var user = findUser(credentials.username)
    if (!user) {
      done(null, false, { message: 'Incorrect username.' })
    } else {
      done(null, user)
    }
  }

  passportStrategy = new JsonStrategy( localCallback )
} else {
  // TODO: Consider placing this initialization strategy into the config object
  passportLoginType = 'dataporten'
  const dpConfig = {
    clientID: config.dataportenClientID,
    clientSecret: config.dataportenClientSecret,
    callbackURL: dataportenCallbackUrl
  }

  const dpCallback = (accessToken, refreshToken, profile, done) => {
    //load user and return done with the user in it.

    // TODO: Add user info to redux state
    console.log(profile)
    return done(null, {
      userId: profile.data.id,
      name: profile.data.displayName,
      emails: profile.data.emails,
      photos: profile.data.photos,
      accessToken: accessToken
    })
  }

  passportStrategy = new DataportenStrategy(dpConfig, dpCallback)
}

Passport.serializeUser((user, done) => {
  done(null, user);
})

Passport.deserializeUser((user, done) => {
  done(null, user);
})

Passport.use(
  passportStrategy
)

app.use(bodyParser.json())

app.use(session({
  secret: 'react and redux rule!!!!',
  resave: false,
  saveUninitialized: false,
  cookie: { maxAge: 60000 }
}))

app.use(Passport.initialize())
app.use(Passport.session())

// Dataporten callback endpoints
app.get(dataportenCallbackUri, Passport.authenticate(passportLoginType, {failWithError: true}),
  (req, res) => {
    res.status(200).json({
      authenticated: req.isAuthenticated(),
      user: req.user
    })
  },
  (err, req, res, next) => {
    res.status(400).json({
      authenticated: req.isAuthenticated(),
      err: err.message
    })
  }
)
app.get(dataportenUri, Passport.authorize(passportLoginType, {failWithError: true}),
  (req, res) => {
    res.status(200).json({
      authenticated: req.isAuthenticated(),
      user: req.user
    })
  },
  (err, req, res, next) => {
    res.status(400).json({
      authenticated: req.isAuthenticated(),
      err: err.message
    })
  }
)

app.use('/login', Passport.authenticate(passportLoginType, {failWithError: true}),
  (req, res) => {
    res.status(200).json({
      authenticated: req.isAuthenticated(),
      user: req.user
    })
  },
  (err, req, res, next) => {
    res.status(400).json({
      authenticated: req.isAuthenticated(),
      err: err.message
    })
  }
)


/* *** END Security *** */

app.use((req, res) => {
  console.log('request')
  if (app.locals.gateway.validCall(req.url)) {
    console.log('-api')
    // We have an api call, lets forward it
    app.locals.gateway.call(req, res)
  } else {
    console.log('-action')
    // Enable standard action support
    const splittedUrlPath = req.url.split('?')[0].split('/').slice(1);
    const { action, params } = mapUrl(actions, splittedUrlPath);
  
    if (action) {
      action(req, params).then((result) => {
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
      })
    } else {
      res.status(404).end('NOT FOUND')
    }
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
  })

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
    })

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
