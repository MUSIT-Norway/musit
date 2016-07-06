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

const renderDoubleTextArea = (index, props) => (
  <ObservationDoubleTextAreaComponent {...props} />
)

const renderFromToNumber = (index, props) => (
  <ObservationFromToNumberCommentComponent {...props} />
)

const renderStatusPercent = (index, props) => (
  <ObservationStatusPercentageComment {...props} />
)

const renderPest = (index, props) => (
  <ObservationPest {...props} />
)

const observationTypeDefinitions = (translate, actions) => {
  return {
    pest: {
      viewLabel: translate('musit.texts.ok'),
      props: {
        id: 'pest',
        translate: translate,
        lifeCycleItems: ['Adult', 'Puppe', 'Puppeskin', 'Larva', 'Egg'],
        onChangeLifeCycle: (e) => console.log(e),
        onChangeCount: (e) => console.log(e),
        onChangeIdentification: (e) => console.log(e),
        onChangeComments: (e) => console.log(e),
        onAddPest: (e) => console.log(e)
      },
      defaultValues: {
        observations: [],
        identificationValue: '',
        commentsValue: ''
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
      defaultValues: {
        statusValue: '',
        volumeValue: '',
        commentValue: ''
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
      defaultValues: {
        leftValue: '',
        rightValue: ''
      },
      render: renderDoubleTextArea
    },
    fromTo: {
      viewLabel: translate('musit.texts.ok'),
      props: {
        translate: translate,
        onChangeFrom: () => actions.changeFrom,
        onChangeTo: () => console.log('TO changed'),
        onChangeComment: () => console.log('Comment changed')
      },
      defaultValues: {
        fromValue: '',
        toValue: '',
        commentValue: ''
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

const defineFromToType = (id, dropdownLabel, fromLabel, fromTooltip, toLabel, toTooltip, commentLabel, commentTooltip,
  onChangeFrom, onChangeTo, onChangeComment) => {
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
        commentTooltip,
        onChangeFrom,
        onChangeTo,
        onChangeComment
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

const defineStatusType = (id, dropdownLabel, statusLabel, statusTooltip,
  statusOptionValues, volumeLabel, volumeTooltip, commentLabel, commentTooltip) => {
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
