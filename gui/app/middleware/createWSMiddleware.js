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

const createScalaMiddleware = (sock) => {
	  return ( ({dispatch}) => {
        sock.onmessage( (message) => {
            const jsonMessage = JSON.parse(message)
            if (jsonMessage.payload) {
              dispatch(Object.assign({}, jsonMessage.payload, {
                type: jsonMessage.remoteType
              }))
            }
        })

        sock.onopen( (message) => {
          console.log('Websocket connected to server...')
        })

        sock.onclose( (message) => {
          console.log('Websocket lost connection to server...')
        })

        sock.onerror( (message) => {
          console.log('Websocket error: '+message)
        })

        return (next) => (action) => {
            if (action.remote) {
              console.log('RemoteAction: '+action)
              sock.emit(action)
            }


            // Call the next dispatch method in the middleware chain.
            let returnValue = next(action)
            return returnValue
        }
    })
}

export default createScalaMiddleware