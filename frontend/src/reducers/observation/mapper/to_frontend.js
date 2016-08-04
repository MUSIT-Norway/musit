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
const wrapAlcoholState = ((s) => {
  switch (s) {
    case 'Uttørket': return 'Uttørket'
    case 'nesten uttørket': return 'Nesten uttørket'
    case 'litt uttørket': return 'Litt uttørket'
    case 'noe uttørket': return 'Noe uttrørket'
    case 'tilfredsstillende': return 'Tilfredsstillende'
    default: return ''
  }
})

const wrap = (be) => {
  const ret = {}
  ret.user = 'Arne And'
  ret.date = '01-01.2012'
  ret.observations = be.subEvents.map((o) => {
    const retobs = {}
    retobs.data = {}
    switch (o.eventType) {
      case 'observationLight':
        retobs.type = 'lux'
        retobs.data.leftValue = o.lysforhold
        retobs.data.rightValue = o.note
        break
      case 'observationGass':
        retobs.type = 'gas'
        retobs.data.leftValue = o.gass
        retobs.data.rightValue = o.note
        break
      case 'observationMugg':
        retobs.type = 'mold'
        retobs.data.leftValue = o.mugg
        retobs.data.rightValue = o.note
        break
      case 'observationRenhold':
        retobs.type = 'cleaning'
        retobs.data.leftValue = o.renhold
        retobs.data.rightValue = o.note
        break
      case 'observationSkallSikring':
        retobs.type = 'skallsikring'
        retobs.data.leftValue = o.skallSikring
        retobs.data.rightValue = o.note
        break
      case 'observationBrannSikring':
        retobs.type = 'brannsikring'
        retobs.data.leftValue = o.brannSikring
        retobs.data.rightValue = o.note
        break
      case 'observationTyveriSikring':
        retobs.type = 'tyverisikring'
        retobs.data.leftValue = o.tyveriSikring
        retobs.data.rightValue = o.note
        break
      case 'observationVannskadeRisiko':
        retobs.type = 'vannskaderisiko'
        retobs.data.leftValue = o.vannskadeRisiko
        retobs.data.rightValue = o.note
        break
      case 'observationInertLuft':
        retobs.type = 'hypoxicAir'
        retobs.data.fromValue = o.inertLuftFrom
        retobs.data.toValue = o.inertLuftTo
        retobs.data.commentValue = o.note
        break
      case 'observationTemperature':
        retobs.type = 'temperature'
        retobs.data.fromValue = o.temperatureFrom
        retobs.data.toValue = o.temperatureTo
        retobs.data.commentValue = o.note
        break
      case 'observationRelativeHumidity':
        retobs.type = 'rh'
        retobs.data.fromValue = o.humidityFrom
        retobs.data.toValue = o.humidityTo
        retobs.data.commentValue = o.note
        break
      case 'observationSkadedyr':
        retobs.type = 'pest'
        retobs.data.identificationValue = o.identifikasjon
        retobs.data.commentsValue = o.note
        retobs.data.observations = o.livssykluser.map((l) => {
          const obs = {}
          obs.lifeCycle = l.livssyklus
          obs.count = l.antall
          return obs
        }
        )
        break
      case 'observationSprit':
        retobs.type = 'alcohol'
        retobs.data.commentValue = o.note
        retobs.data.statusValue = wrapAlcoholState(o.tilstand)
        retobs.data.volumeValue = o.volum
        break
      default:
    }
    return retobs
  })
  return ret
}
const toFrontEnd = (be) => {
  return wrap(be)
}

export default toFrontEnd
