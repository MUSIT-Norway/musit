

const initialState = {
  data: {
    row_1: {
      nodeName: 'Eske',
      nodeType: 'Lagringsenh',
      objectCount: 0,
      totalObjectCount: 12,
      nodeCount: 0
    },
    row_2: {
      nodeName: 'Pose',
      nodeType: 'Lagringsenh',
      objectCount: 0,
      totalObjectCount: 16,
      nodeCount: 0
    },
    row_3: {
      nodeName: 'Kurv',
      nodeType: 'Lagringsenh',
      objectCount: 0,
      totalObjectCount: 8,
      nodeCount: 0
    },
    row_4: {
      nodeName: 'Boks',
      nodeType: 'Lagringsenh',
      objectCount: 0,
      totalObjectCount: 7,
      nodeCount: 0
    },
    row_5: {
      nodeName: 'Dingseboms',
      nodeType: 'Lagringsenh',
      objectCount: 11,
      totalObjectCount: 9,
      nodeCount: 0
    }

  }
}


const nodeGridReducer = (state = initialState) => {
  return state;
}

export default nodeGridReducer
