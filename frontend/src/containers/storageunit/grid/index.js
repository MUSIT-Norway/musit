import { connect } from 'react-redux';
import Language from '../../../components/language'
import StorageUnitsImpl from './StorageUnits'

const mapStateToProps = () => ({
  translate: (key, markdown) => Language.translate(key, markdown)
})

@connect(mapStateToProps)
export class StorageUnitsContainer extends StorageUnitsImpl {}
