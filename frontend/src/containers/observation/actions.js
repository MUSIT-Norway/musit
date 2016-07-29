export const actions = (thiz) => {
  return {
    changeTempFrom: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'temperature') {
            retVal = { ...o, data: { ...o.data, fromValue: v } }
          }
          return retVal
        })
      })
    },
    changeTempTo: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'temperature') {
            retVal = { ...o, data: { ...o.data, toValue: v } }
          }
          return retVal
        })
      })
    },
    changeTempComment: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'temperature') {
            retVal = { ...o, data: { ...o.data, commentValue: v } }
          }
          return retVal
        })
      })
    },
    changeHypoxicAirFrom: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'hypoxicAir') {
            retVal = { ...o, data: { ...o.data, fromValue: v } }
          }
          return retVal
        })
      })
    },
    changeHypoxicAirTo: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'hypoxicAir') {
            retVal = { ...o, data: { ...o.data, toValue: v } }
          }
          return retVal
        })
      })
    },
    changeHypoxicAirComment: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'hypoxicAir') {
            retVal = { ...o, data: { ...o.data, commentValue: v } }
          }
          return retVal
        })
      })
    },
    changeRHFrom: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'rh') {
            retVal = { ...o, data: { ...o.data, fromValue: v } }
          }
          return retVal
        })
      })
    },
    changeRHTo: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'rh') {
            retVal = { ...o, data: { ...o.data, toValue: v } }
          }
          return retVal
        })
      })
    },
    changeRHComment: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'rh') {
            retVal = { ...o, data: { ...o.data, commentValue: v } }
          }
          return retVal
        })
      })
    },
    changeLuxLeft: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'lux') {
            retVal = { ...o, data: { ...o.data, leftValue: v } }
          }
          return retVal
        })
      })
    },
    changeLuxRight: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'lux') {
            retVal = { ...o, data: { ...o.data, rightValue: v } }
          }
          return retVal
        })
      })
    },
    changeGasLeft: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'gas') {
            retVal = { ...o, data: { ...o.data, leftValue: v } }
          }
          return retVal
        })
      })
    },
    changeGasRight: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'gas') {
            retVal = { ...o, data: { ...o.data, rightValue: v } }
          }
          return retVal
        })
      })
    },
    changeCleaningLeft: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'cleaning') {
            retVal = { ...o, data: { ...o.data, leftValue: v } }
          }
          return retVal
        })
      })
    },
    changeCleaningRight: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'cleaning') {
            retVal = { ...o, data: { ...o.data, rightValue: v } }
          }
          return retVal
        })
      })
    },
    changeMoldLeft: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'mold') {
            retVal = { ...o, data: { ...o.data, leftValue: v } }
          }
          return retVal
        })
      })
    },
    changeMoldRight: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'mold') {
            retVal = { ...o, data: { ...o.data, rightValue: v } }
          }
          return retVal
        })
      })
    },
    changeSkallSikringLeft: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'skallsikring') {
            retVal = { ...o, data: { ...o.data, leftValue: v } }
          }
          return retVal
        })
      })
    },
    changeSkallSikringRight: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'skallsikring') {
            retVal = { ...o, data: { ...o.data, rightValue: v } }
          }
          return retVal
        })
      })
    },
    changeTyveriSikringLeft: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'tyverisikring') {
            retVal = { ...o, data: { ...o.data, leftValue: v } }
          }
          return retVal
        })
      })
    },
    changeTyveriSikringRight: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'tyverisikring') {
            retVal = { ...o, data: { ...o.data, rightValue: v } }
          }
          return retVal
        })
      })
    },
    changeBrannSikringLeft: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'brannsikring') {
            retVal = { ...o, data: { ...o.data, leftValue: v } }
          }
          return retVal
        })
      })
    },
    changeBrannSikringRight: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'brannsikring') {
            retVal = { ...o, data: { ...o.data, rightValue: v } }
          }
          return retVal
        })
      })
    },
    changeVannskadeRisikoLeft: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'vannskaderisiko') {
            retVal = { ...o, data: { ...o.data, leftValue: v } }
          }
          return retVal
        })
      })
    },
    changeVannskadeRisikoRight: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'vannskaderisiko') {
            retVal = { ...o, data: { ...o.data, rightValue: v } }
          }
          return retVal
        })
      })
    },
    changeAlchoholStatus: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'alcohol') {
            retVal = { ...o, data: { ...o.data, statusValue: v } }
          }
          return retVal
        })
      })
    },
    changeAlchoholVolume: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'alcohol') {
            retVal = { ...o, data: { ...o.data, volumeValue: v } }
          }
          return retVal
        })
      })
    },
    changeAlchoholComment: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'alcohol') {
            retVal = { ...o, data: { ...o.data, commentValue: v } }
          }
          return retVal
        })
      })
    },
    addPest: () => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          const p = { lifeCycle: '', count: 0 }
          if (o.type === 'pest') {
            retVal = { ...o, data: { ...o.data, observations: [...o.data.observations, p] } }
          }
          return retVal
        })
      })
    },
    changeLifeCycle: (i, v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'pest') {
            const po = o.data.observations
            po[i] = { ...po[i], lifeCycle: v }
            retVal = { ...o, data: { ...o.data, observations: po } }
          }
          return retVal
        })
      })
    },
    changeCount: (i, v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'pest') {
            const po = o.data.observations
            po[i] = { ...po[i], count: v }
            retVal = { ...o, data: { ...o.data, observations: po } }
          }
          return retVal
        })
      })
    },
    changePestIdentification: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'pest') {
            retVal = { ...o, data: { ...o.data, identificationValue: v } }
          }
          return retVal
        })
      })
    },
    changePestComment: (v) => {
      thiz.setState({
        ...thiz.state, observations: thiz.state.observations.map((o) => {
          let retVal = o
          if (o.type === 'pest') {
            retVal = { ...o, data: { ...o.data, commentsValue: v } }
          }
          return retVal
        })
      })
    }
  }
}
