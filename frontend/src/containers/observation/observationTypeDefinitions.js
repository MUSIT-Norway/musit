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

const observationTypeDefinitions = (translate) => {
  return {
    pest: {
      viewLabel: translate('musit.texts.ok'),
      props: {
        id: 'pest',
        translate: translate,
        lifeCycleItems: ['Adult', 'Puppe', 'Puppeskin', 'Larva', 'Egg']
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
        translateKeyPrefix: 'musit.storageUnits.environmentRequirements.alcohol.statusItems.'
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
        translate: translate
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
        translate: translate
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

const defineCommentType = (id, dropdownLabel, leftLabel, leftTooltip, rightLabel, rightTooltip,
                          onChangeLeft, onChangeRight) => {
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
        onChangeLeft,
        onChangeRight
      }
    }
  }
}

const defineFromToType = (id, dropdownLabel, fromLabel, fromTooltip, toLabel, toTooltip, commentLabel, commentTooltip,
  onChangeFrom, onChangeTo, onChangeComment, fromPlaceHolder, toPlaceHolder) => {
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
        onChangeComment,
        fromPlaceHolder,
        toPlaceHolder
      }
    }
  }
}

const definePestType = (id, dropdownLabel, onAddPest, onChangeLifeCycle, onChangeCount,
  onChangeIdentification, onChangeComments) => {
  return {
    label: dropdownLabel,
    component: {
      viewType: 'pest',
      props: {
        id,
        onAddPest,
        onChangeLifeCycle,
        onChangeCount,
        onChangeIdentification,
        onChangeComments
      }
    }
  }
}

const defineStatusType = (id, dropdownLabel, statusLabel, statusTooltip,
  statusOptionValues, volumeLabel, volumeTooltip, commentLabel, commentTooltip,
  onChangeStatus, onChangeVolume, onChangeComment) => {
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
        commentTooltip,
        onChangeStatus,
        onChangeVolume,
        onChangeComment
      }
    }
  }
}

export { observationTypeDefinitions, defineCommentType, defineFromToType, definePestType, defineStatusType }
