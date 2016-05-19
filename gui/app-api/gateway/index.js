import NodeCache from 'node-cache'
import request from 'request'

export default class APIGateway {
  constructor(baseUri) {
    this.baseUri = baseUri
    this.registry = new NodeCache()
    const msConfig = require('./services.json')
    for (var microservice of msConfig) {
      this.registry.set(microservice.name, microservice)
    }
  }

  validCall = (uri) => uri && uri.indexOf(this.baseUri) == 0

  call = (req, res) => {
    let uriArray = req.url.split('?')
    let splitArray = uriArray[0].split('/')
    this.registry.get(splitArray[2], (err, service) => {
      if (!err && service) {
        console.log('-- calling service: ' + service.name)
        // Fix options
        let options = ''
        if (uriArray[1] && uriArray[1].length > 0) {
          options = '?' + uriArray[1]
        }

        // Fix uri
        let uri = ''
        let uriString = splitArray.slice(3).join('/')
        if (uriString && uriString.length > 0) {
          uri = '/' + uriString
        }

        // Build url
        let newUrl = service.protocol + service.host + ':' + service.port + uri + options;

        // Pipe the request to the microservices
        console.log('Forwarding service call in pipe: ' + newUrl)
        let newReq = request(newUrl)
        newReq.on('error', (err) => {
          console.error('MS ERROR:', err)
          res.status(500).json(err)
        })

        req.pipe(newReq).pipe(res)
      } else if (!err) {
        console.error('MS ERROR:', err)
        res.status(404).json(err)
      } else {
        console.error('MS ERROR: Service not found')
        res.status(404)
      }
    })

  }
}
