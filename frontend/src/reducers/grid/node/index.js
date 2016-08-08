const LOAD_NODE = 'MUSIT/GRID/LOAD_NODE'

const initialState = {
  data: {
    row_1: {
      id: 1,
      name: 'Eske',
      storageType: 'Lagringsenh',
      objectCount: 0,
      totalObjectCount: 12,
      nodeCount: 0
    },
    row_2: {
      id: 2,
      name: 'Pose',
      storageType: 'Lagringsenh',
      objectCount: 0,
      totalObjectCount: 16,
      nodeCount: 0
    },
    row_3: {
      id: 3,
      name: 'Kurv',
      storageType: 'Lagringsenh',
      objectCount: 0,
      totalObjectCount: 8,
      nodeCount: 0
    },
    row_4: {
      id: 4,
      name: 'Boks',
      storageType: 'Lagringsenh',
      objectCount: 0,
      totalObjectCount: 7,
      nodeCount: 0
    },
    row_5: {
      id: 5,
      name: 'Dingseboms',
      storageType: 'Lagringsenh',
      objectCount: 11,
      totalObjectCount: 9,
      nodeCount: 0
    }/* ,
    real_1: {
      id: 1,
      name: 'String',
      area: 1,
      areaTo: 1,
      isPartOf: 1,
      height: 1,
      heightTo: 1,
      groupRead: 'Option[String]',
      groupWrite: 'Option[String]',
      links: 'Option[Seq[Link]]',
      storageType: 'StorageType'
    }*/
  }
}


const nodeGridReducer = (state = initialState) => {
  return state
}

export const loadNode = (id) => {
  console.log('dispatching load for node '+id)
  return {
    type: LOAD_NODE,
    id: id
  }
}

export default nodeGridReducer
