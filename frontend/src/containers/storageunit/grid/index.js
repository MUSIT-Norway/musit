import { connect } from 'react-redux';
import Language from '../../../components/language'
import StorageUnitsContainerImpl from './StorageUnitsContainer'

const mapStateToProps = () => ({
  translate: (key, markdown) => Language.translate(key, markdown)
})

@connect(mapStateToProps)
export class StorageUnitsContainer extends StorageUnitsContainerImpl {}
