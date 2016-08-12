import React from 'react'
import FontAwesome from 'react-fontawesome'
import styles from './Breadcrumb.scss'

export default class Breadcrumb extends React.Component {
  static propTypes = {
    nodes: React.PropTypes.arrayOf(React.PropTypes.shape({
      id: React.PropTypes.number.isRequired,
      name: React.PropTypes.string.isRequired,
      type: React.PropTypes.string,
      url: React.PropTypes.string
    })),
    nodeTypes: React.PropTypes.arrayOf(React.PropTypes.shape({
      type: React.PropTypes.string,
      iconName: React.PropTypes.string
    })),
    onClickCrumb: React.PropTypes.func.isRequired
  }

  render() {
    const {
      nodes,
      nodeTypes,
      onClickCrumb
    } = this.props

    const isLast = (array, index) => (array.length - 1) === index
    const renderCrumb = (nodeArray) => {
      const breadcrumb = nodes.map((node, index) => {
        let fragment = ''
        let iconFragment = ''
        if (node.type && nodeTypes) {
          const currentType = nodeTypes.find((nodeType) => nodeType.type === node.type)
          if (currentType) {
            iconFragment = (
              <FontAwesome name={currentType.iconName} style={{ padding: '2px' }} />
            )
          }
        }
        if (!isLast(nodeArray, index)) {
          fragment = (
            <span>
              <span className={styles.crumb}>
                <a
                  href=""
                  onClick={(e) => {
                    e.preventDefault()
                    onClickCrumb(node, index)
                  }}
                >{iconFragment}{node.name}</a>
              </span>
              <span className={styles.crumb}>/</span>
            </span>
          )
        } else {
          fragment = (<span className={styles.crumb}>{node.name}</span>)
        }
        return fragment
      })
      return breadcrumb
    }
    return (
      <div>
        <span>/</span>{renderCrumb(nodes)}
      </div>
    )
  }
}
