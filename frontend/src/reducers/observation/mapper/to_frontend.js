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
  const obs = be.subEvents
  const ret = {}
  ret.data = {}
  obs.map((o) => {
    const retobs = {}
    switch (o.eventType) {
      case 'observationLight':
        retobs.data.type = 'lux'
        retobs.data.leftValue = o.Lysforhold
        retobs.data.rightValue = o.note
        break
      case 'observationGass':
        retobs.data.type = 'gas'
        retobs.data.leftValue = o.Gass
        retobs.data.rightValue = o.note
        break
      case 'observationMugg':
        retobs.data.type = 'mold'
        retobs.data.leftValue = o.Mugg
        retobs.data.rightValue = o.note
        break
      case 'observationRenhold':
        retobs.data.type = 'cleaning'
        retobs.data.leftValue = o.Renhold
        retobs.data.rightValue = o.note
        break
      case 'observationSkallSikring':
        retobs.data.type = 'skallsikring'
        retobs.data.leftValue = o.Skallsikring
        retobs.data.rightValue = o.note
        break
      case 'observationBrannSikring':
        retobs.data.type = 'brannsikring'
        retobs.data.leftValue = o.BrannSikring
        retobs.data.rightValue = o.note
        break
      case 'observationTyveriSikring':
        retobs.data.type = 'tyverisikring'
        retobs.data.leftValue = o.TyveriSikring
        retobs.data.rightValue = o.note
        break
      case 'observationVannskadeRisiko':
        retobs.data.type = 'vannskaderisiko'
        retobs.data.leftValue = o.VannskadeRisiko
        retobs.data.rightValue = o.note
        break
      case 'observationInertLuft':
        retobs.data.type = 'hypoxicAir'
        retobs.data.fromValue = o.inertLuft_from
        retobs.data.toValue = o.inertLuft_to
        retobs.data.commentValue = o.note
        break
      case 'observationTemperature':
        retobs.data.type = 'hypoxicAir'
        retobs.data.fromValue = o.temperature_from
        retobs.data.toValue = o.temperature_to
        retobs.data.commentValue = o.note
        break
      case 'observationRelativeHumidity':
        retobs.data.type = 'hypoxicAir'
        retobs.data.fromValue = o.humidity_from
        retobs.data.toValue = o.humidity_to
        retobs.data.commentValue = o.note
        break
      case 'observationSkadedyr':
      case 'observationSprit':
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
