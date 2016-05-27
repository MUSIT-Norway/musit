/**
 * Created by steinaol on 5/27/16.
 */



import React, { Component } from 'react';
import TextField from '../../components/musittextfield';
import { Panel, Form, Grid, Row, Col, } from 'react-bootstrap'

export default class StorageUnitComponent extends Component {
    static validateString(value, minimumLength = 3, maximumLength = 20) {
        const isSomething = value.length >= minimumLength
        const isValid = isSomething ? 'success' : null
        return value.length > maximumLength ? 'error' : isValid
    }

    static validateNumber(value, minimumLength = 1) {
        const isSomething = value.length >= minimumLength
        const isValid = isSomething ? 'success' : null
        return isSomething && isNaN(value) ? 'error' : isValid
    }

    constructor(props) {
        super(props)

        this.state = {
            storageunit: {
                type: 'Lagringsenhet',
                name: 'navn',
                areal1: '',
                areal2: '',
                hoyde1: '2',
                hoyde2: '4'
            }
        }

        const clazz = this

        this.areal1 = {
            controlId: 'areal1',
            labelText: 'Areal-1',
            tooltip: 'Areal',
            valueType: 'text',
            placeHolderText: 'enter areal 1 here',
            valueText: () => clazz.state.storageunit.areal1,
            validationState: () => StorageUnitComponent.validateNumber(clazz.state.storageunit.areal1),
            onChange: (areal) => clazz.setState({ storageunit: { ...this.state.storageunit, areal } })
        }

        this.areal2 = {
            controlId: 'areal2',
            labelText: 'Areal-2',
            tooltip: 'Areal',
            valueType: 'text',
            placeHolderText: 'enter areal here',
            valueText: () => clazz.state.unit.areal,
            validationState: () => StorageUnitComponent.validateNumber(clazz.state.storageunit.areal),
            onChange: (areal) => clazz.setState({ storageunit: { ...this.state.storageunit, areal } })
        }

        this.hoyde1 = {
            controlId: 'hoyde1',
            labelText: 'Høyde (1)',
            tooltip: 'Høyde (1)',
            valueType: 'text',
            placeHolderText: 'enter høyde 1 here',
            valueText: () => clazz.state.storageunit.hoyde1,
            validationState: () => StorageUnitComponent.validateNumber(clazz.state.unit.hoyde1),
            onChange: (hoyde) => clazz.setState({ storageunit: { ...this.state.unit, hoyde } })
        }


        this.hoyde2 = {
            controlId: 'hoyde2',
            labelText: 'Høyde (2)',
            tooltip: 'Høyde (2)',
            valueType: 'text',
            placeHolderText: 'enter høyde 2 here',
            valueText: () => clazz.state.storageunit.hoyde2,
            validationState: () => StorageUnitComponent.validateNumber(clazz.state.storageunit.hoyde2),
            onChange: (hoyde) => clazz.setState({ storageunit: { ...this.state.storageunit, hoyde } })
        }





        this.type =
            {
                controlId: 'storageUnitType',
                labelText: 'Type',
                tooltip: 'Type lagringsenhet',
                valueType: 'text',
                placeHolderText: 'velg type here',
                valueText: () => clazz.state.storageunit.type,
                validationState: () => StorageUnitComponent.validateString(clazz.state.storageunit.type),
                onChange: (type) => clazz.setState({ storageunit: { ...clazz.state.storageunit, type } })
            }
        this.name =
            {
                controlId: 'name',
                labelText: 'Navn',
                tooltip: 'Navn',
                placeHolderText: 'enter name here',
                valueType: 'text',
                valueText: () => clazz.state.storageunit.name,
                validationState: () => StorageUnitComponent.validateString(clazz.state.storageunit.description),
                onChange: (description) => clazz.setState({ storageunit: { ...clazz.state.storageunit, description } })
            }
    }

    render() {
        return (
            <div>
                <main>
                    <Panel>
                        <Grid>
                            <Row styleClass="row-centered">

                                <Col md={6}>
                                    <Form horizontal>
                                        {this.fields.map(field => <TextField {...field} />)}
                                    </Form>
                                </Col>
                                <Col md={6}>
                                    <Form horizontal>
                                        <TextField {...this.areal} />
                                        <TextField {...this.areal} />
                                        <TextField {...this.areal} />
                                    </Form>
                                </Col>
                            </Row>
                        </Grid>
                    </Panel>
                </main>
            </div>
        );
    }
}
