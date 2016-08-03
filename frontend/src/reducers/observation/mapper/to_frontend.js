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

const wrap = (be) => {
  const ret = {}
  ret.data = {}
  ret.data.observations = be.subEvents.map((o) => {
    const retobs = {}
    switch (o.eventType) {
      case 'observationLight':
        retobs.type = 'lux'
        retobs.leftValue = o.Lysforhold
        retobs.rightValue = o.note
        break
      case 'observationGass':
        retobs.type = 'gas'
        retobs.leftValue = o.Gass
        retobs.rightValue = o.note
        break
      case 'observationMugg':
        retobs.type = 'mold'
        retobs.leftValue = o.Mugg
        retobs.rightValue = o.note
        break
      case 'observationRenhold':
        retobs.type = 'cleaning'
        retobs.leftValue = o.Renhold
        retobs.rightValue = o.note
        break
      case 'observationSkallSikring':
        retobs.type = 'skallsikring'
        retobs.leftValue = o.Skallsikring
        retobs.rightValue = o.note
        break
      case 'observationBrannSikring':
        retobs.type = 'brannsikring'
        retobs.leftValue = o.BrannSikring
        retobs.rightValue = o.note
        break
      case 'observationTyveriSikring':
        retobs.type = 'tyverisikring'
        retobs.leftValue = o.TyveriSikring
        retobs.rightValue = o.note
        break
      case 'observationVannskadeRisiko':
        retobs.type = 'vannskaderisiko'
        retobs.leftValue = o.VannskadeRisiko
        retobs.rightValue = o.note
        break
      case 'observationInertLuft':
        retobs.type = 'hypoxicAir'
        retobs.fromValue = o.inertLuft_from
        retobs.toValue = o.inertLuft_to
        retobs.commentValue = o.note
        break
      case 'observationTemperature':
        retobs.type = 'hypoxicAir'
        retobs.fromValue = o.temperature_from
        retobs.toValue = o.temperature_to
        retobs.commentValue = o.note
        break
      case 'observationRelativeHumidity':
        retobs.type = 'hypoxicAir'
        retobs.fromValue = o.humidity_from
        retobs.toValue = o.humidity_to
        retobs.commentValue = o.note
        break
      case 'observationSkadedyr':
        retobs.type = 'pest'
        retobs.identificationValue = o.identifikasjon
        retobs.observations = o.livssykluser.map((l) => {
          const obs = {}
          obs.lifeCycle = l.livssyklus
          obs.count = l.antall
          return obs
        }
      )
        break
      case 'observationSprit':
        retobs.type = 'alcohol'
        retobs.commentValue = o.note
        retobs.statusValue = o.Tilstander[0].Tilstand
        retobs.volume = o.Tilstander[0].Volum
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
