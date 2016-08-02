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

/* Frontend:
 doneBy; <doneBy>
 date; <date>
 [

]

*/


const wrap = (e) => {
  const r = {}
  r.subEvents = e.observations.map((el) => {
    const re = {}
    switch (el.type) {
      case 'pest':
        re.eventType = 'observationSkadedyr'
        re.identifikasjon = el.data.identificationValue
        re.note = el.data.commentsValue
        re.livssykluser = el.data.observations.map((o) => {
          const ret = {}
          ret.livssyklus = o.lifeCycle
          ret.antall = o.count
          return ret
        })
        break
      case 'lux':
        re.eventType = 'observationLight'
        re.Lysforhold = el.data.leftValue
        re.note = el.data.rightValue
        break
      case 'gas':
        re.eventType = 'observationGass'
        re.Gass = el.data.leftValue
        re.note = el.data.rightValue
        break
      case 'cleaning':
        re.eventType = 'observationRenhold'
        re.Renhold = el.data.leftValue
        re.note = el.data.rightValue
        break
      case 'rh':
        re.eventType = 'observationRelativeHumidity'
        re.humidity_from = el.data.fromValue
        re.humidity_to = el.data.toValue
        break
      case 'mold':
        re.eventType = 'observationMugg'
        re.Mugg = el.data.leftValue
        re.note = el.data.rightValue
        break
      case 'skallsikring':
        re.eventType = 'observationSkallSikring'
        re.Skallsikring = el.data.leftValue
        re.note = el.data.rightValue
        break
      case 'tyverisikring':
        re.eventType = 'observationTyveriSikring'
        re.TyveriSikring = el.data.leftValue
        re.note = el.data.rightValue
        break
      case 'brannsikring':
        re.eventType = 'observationBrannSikring'
        re.BrannSikring = el.data.leftValue
        re.note = el.data.rightValue
        break
      case 'vannskaderisiko':
        re.eventType = 'observationVannskadeRisiko'
        re.VannskadeRisiko = el.data.leftValue
        re.note = el.data.rightValue
        break
      case 'hypoxicAir':
        re.eventType = 'observationInertLuft'
        re.inertLuft_from = el.data.fromValue
        re.inertLuft_to = el.data.toValue
        break
      case 'alcohol':
        re.eventType = 'observationSprit'
        re.note = el.data.commentValue
        re.Tilstander = [{ Tilstand: el.data.statusValue, Volum: el.data.volumeValue }]
        re.Tilstander[0].Volum = el.data.volumeValue
        break
      case 'temperature':
        re.eventType = 'observationTemperature'
        re.temperature_from = el.data.fromValue
        re.temperature_to = el.data.toValue
        break
      default:
    }
    return re
  }
  )
  return r
}


const toBackEnd = (fe) => {
  return wrap(fe)
}

export default toBackEnd
