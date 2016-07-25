

import React, { Component, PropTypes } from 'react'
import { PageHeader, Row, Col, Table, FormGroup } from 'react-bootstrap'
import FontAwesome from 'react-fontawesome'

export default class ObservationControlGrid extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    translate: PropTypes.func.isRequired,
    tableData: PropTypes.arrayOf(PropTypes.shape({
      type: PropTypes.string.isRequired,
      date: PropTypes.string.isRequired,
      types: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.number,
        temperature: PropTypes.bool,
        inertAir: PropTypes.bool,
        relativeHumidity: PropTypes.bool,
        cleaning: PropTypes.bool,
        lightCondition: PropTypes.bool,
        alchohol: PropTypes.bool,
        gas: PropTypes.bool,
        mold: PropTypes.bool,
        pest: PropTypes.bool,
        envdata: PropTypes.bool
      })),
      doneBy: PropTypes.string.isRequired,
      registeredDate: PropTypes.string.isRequired,
      registeredBy: PropTypes.string.isRequired
    }))
  }
  static iconMap = {
    temperature: 'asterisk',
    inertAir: 'cloud',
    relativeHumidity: 'tint',
    cleaning: 'fa',
    lightCondition: 'sun-o',
    alchohol: 'percent',
    gas: 'inr',
    mold: 'bolt',
    pest: 'bug',
    envdata: 'truck'
  }

  render() {
    const { id, translate } = this.props
    const showEnabledIcon = (data, type) => {
      return (
        (data) ? <FontAwesome style={{ padding: '2px' }} name={ObservationControlGrid.iconMap[type]} /> : ''
      )
    }
    const showDisabledIcon = (data, type) => {
      return (
        (data === false) ?
          <FontAwesome style={{ color: 'gray', padding: '2px' }} name={ObservationControlGrid.iconMap[type]} /> : ''
      )
    }
    return (
      <FormGroup>
        <div>
          <Row>
            <Col style={{ textAlign: 'center' }}>
              <PageHeader>
                {`${id}${translate('musit.grid.observation.header')}`}
              </PageHeader>
            </Col>
          </Row>
          <Table responsive hover condensed>
            <thead>
              <tr>
                <th>
                </th>
                <th>
                  {translate('musit.grid.observation.date')}
                </th>
                <th>
                  {translate('musit.grid.observation.types')}
                </th>
                <th>
                  {translate('musit.grid.observation.doneBy')}
                </th>
                <th>
                  {translate('musit.grid.observation.registeredDate')}
                </th>
                <th>
                  {translate('musit.grid.observation.registeredBy')}
                </th>
                <th>
                </th>
              </tr>
            </thead>
            <tbody>
              {this.props.tableData.map(c =>
                <tr id={`${id}_${c.date}`} >
                  <td id={`${id}_${c.date}_type`}>
                    {c.type === 'control' ? <FontAwesome name="user-secret" /> : ''}
                    {c.type === 'observation' ? <FontAwesome name="eye" /> : ''}
                  </td>
                  <td id={`${id}_${c.date}_date`}>
                    {`${c.date}`}
                  </td>
                  <td id={`${id}_${c.date}_types`}>
                    {showEnabledIcon(c.types.temperature, 'temperature')}
                    {showDisabledIcon(c.types.temperature, 'temperature')}
                    {showEnabledIcon(c.types.inertAir, 'inertAir')}
                    {showDisabledIcon(c.types.inertAir, 'inertAir')}
                    {showEnabledIcon(c.types.relativeHumidity, 'relativeHumidity')}
                    {showDisabledIcon(c.types.relativeHumidity, 'relativeHumidity')}
                    {showEnabledIcon(c.types.cleaning, 'cleaning')}
                    {showDisabledIcon(c.types.cleaning, 'cleaning')}
                    {showEnabledIcon(c.types.lightCondition, 'lightCondition')}
                    {showDisabledIcon(c.types.lightCondition, 'lightCondition')}
                    {showEnabledIcon(c.types.alchohol, 'alchohol')}
                    {showDisabledIcon(c.types.alchohol, 'alchohol')}
                    {showEnabledIcon(c.types.gas, 'gas')}
                    {showDisabledIcon(c.types.gas, 'gas')}
                    {showEnabledIcon(c.types.mold, 'mold')}
                    {showDisabledIcon(c.types.mold, 'mold')}
                    {showEnabledIcon(c.types.pest, 'pest')}
                    {showDisabledIcon(c.types.pest, 'pest')}
                    {showEnabledIcon(c.types.envdata, 'envdata')}
                    {showDisabledIcon(c.types.envdata, 'envdata')}
                  </td>
                  <td id={`${id}_${c.date}_doneBy`}>
                    {`${c.doneBy}`}
                  </td>
                  <td id={`${id}_${c.date}_registeredDate`}>
                    {`${c.registeredDate}`}
                  </td>
                  <td id={`${id}_${c.date}_registeredBy`}>
                    {`${c.registeredBy}`}
                  </td>
                  <td id={`${id}_${c.museumsNumber}_${c.uNumber}_newspaper`}>
                    <FontAwesome name="newspaper-o" />
                  </td>
                </tr>
              )}
            </tbody>
          </Table>
        </div>
      </FormGroup>
    )
  }
}
