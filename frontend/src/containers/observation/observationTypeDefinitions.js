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

import React from 'react'
import {
  ObservationDoubleTextAreaComponent,
  ObservationFromToNumberCommentComponent,
  ObservationStatusPercentageComment,
  ObservationPest
} from '../../components/observation'

const renderDoubleTextArea = (props) => (
  <ObservationDoubleTextAreaComponent {...props} />
)

const renderFromToNumber = (props) => (
  <ObservationFromToNumberCommentComponent {...props} />
)

const renderStatusPercent = (props) => (
  <ObservationStatusPercentageComment {...props} />
)

const renderPest = (props) => (
  <ObservationPest {...props} />
)

const observationTypeDefinitions = (translate, addPest) => {
  return {
    pest: {
      viewLabel: translate('musit.texts.ok'),
      props: {
        id: 'pest',
        translate: translate,
        lifeCycleItems: ['Adult', 'Puppe', 'Puppeskin', 'Larva', 'Egg'],
        onChangeLifeCycle: (index, value) => console.log(`${index}:${value}`),
        onChangeCount: (index, value) => console.log(`${index}:${value}`),
        onChangeIdentification: () => console.log('Comment changed'),
        onChangeComments: () => console.log('Comment changed'),
        onAddPest: () => addPest()
      },
      render: renderPest
    },
    status: {
      viewLabel: translate('musit.texts.ok'),
      props: {
        translate: translate,
        onChangeStatus: () => console.log('From changed'),
        onChangeVolume: () => console.log('TO changed'),
        onChangeComment: () => console.log('Comment changed')
      },
      render: renderStatusPercent
    },
    comments: {
      viewLabel: translate('musit.texts.ok'),
      props: {
        translate: translate,
        onChangeLeft: () => console.log('left changed'),
        onChangeRight: () => console.log('Right changed')
      },
      render: renderDoubleTextArea
    },
    fromTo: {
      viewLabel: translate('musit.texts.ok'),
      props: {
        translate: translate,
        onChangeFrom: () => console.log('From changed'),
        onChangeTo: () => console.log('TO changed'),
        onChangeComment: () => console.log('Comment changed')
      },
      render: renderFromToNumber
    }
  }
}

const defineCommentType = (id, dropdownLabel, leftLabel, leftTooltip, rightLabel, rightTooltip) => {
  return {
    label: dropdownLabel,
    component: {
      viewType: 'comments',
      props: {
        id,
        leftLabel,
        leftTooltip,
        rightLabel,
        rightTooltip,
      }
    }
  }
}

const defineFromToType = (id, dropdownLabel, fromLabel, fromTooltip, toLabel, toTooltip, commentLabel, commentTooltip) => {
  return {
    label: dropdownLabel,
    component: {
      viewType: 'fromTo',
      props: {
        id,
        fromLabel,
        fromTooltip,
        toLabel,
        toTooltip,
        commentLabel,
        commentTooltip
      }
    }
  }
}

const definePestType = (id, dropdownLabel) => {
  return {
    label: dropdownLabel,
    component: {
      viewType: 'pest',
      props: {
        id
      }
    }
  }
}

const defineStatusType = (id, dropdownLabel, statusLabel, statusTooltip, statusOptionValues, volumeLabel, volumeTooltip, commentLabel, commentTooltip) => {
  return {
    label: dropdownLabel,
    component: {
      viewType: 'status',
      props: {
        id,
        statusLabel,
        statusTooltip,
        statusOptionValues,
        volumeLabel,
        volumeTooltip,
        commentLabel,
        commentTooltip
      }
    }
  }
}

export { observationTypeDefinitions, defineCommentType, defineFromToType, definePestType, defineStatusType }
